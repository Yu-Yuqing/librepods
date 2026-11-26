package me.kavishdevar.librepods.data.wear

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** One process-wide queue owns recording and queries, independently of any Activity. */
class WearStatisticsRepository private constructor(context: Context) {
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val database = WearDatabase(context.applicationContext)
    private val _changes = MutableStateFlow(0L)
    val changes = _changes.asStateFlow()

    private val recorder by lazy {
        // An interrupted service cannot tell when the user removed the buds. Close only at
        // the last durable checkpoint; never count the unobserved gap after a process death.
        database.writableDatabase.execSQL(
            "UPDATE sessions SET end_ms = last_observed_ms WHERE end_ms IS NULL"
        )
        WearSessionRecorder(database)
    }

    fun earState(wearing: Boolean) {
        val wall = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        enqueue {
            val previousId = recorder.activeId
            recorder.earState(wearing, wall, elapsed)
            previousId != recorder.activeId
        }
    }

    fun checkpoint() {
        val elapsed = SystemClock.elapsedRealtime()
        enqueue {
            recorder.checkpoint(elapsed)
            false
        }
    }

    fun stop(elapsedMillis: Long = SystemClock.elapsedRealtime()) {
        enqueue {
            val wasActive = recorder.activeId != null
            recorder.stop(elapsedMillis)
            wasActive
        }
    }

    private fun enqueue(action: () -> Boolean) {
        scope.launch {
            try {
                if (action()) _changes.value++
            } catch (exception: Exception) {
                // Storage failure must not take down Bluetooth control or media playback.
                Log.e("WearStatistics", "Could not save wear session", exception)
            }
        }
    }

    suspend fun report(period: WearPeriod, date: LocalDate, zone: ZoneId): WearReport =
        withContext(dispatcher) {
            val active = recorder.currentSession(SystemClock.elapsedRealtime())
            val start = period.startDate(date).atStartOfDay(zone).toInstant().toEpochMilli()
            val end = period.endDate(period.startDate(date)).atStartOfDay(zone).toInstant().toEpochMilli()
            val sessions = database.read(start, end).map {
                if (active != null && it.id == active.id) active else it
            }.toMutableList()
            // A newly started live session may not have a nonzero saved duration yet.
            if (active != null && sessions.none { it.id == active.id }) sessions.add(active)
            WearStatistics.report(sessions, period, date, zone).copy(
                activeSessionId = active?.id,
                activeDurationMillis = active?.let { it.endMillis - it.startMillis } ?: 0,
            )
        }

    internal suspend fun snapshot(): List<WearSession> = withContext(dispatcher) {
        val active = recorder.currentSession(SystemClock.elapsedRealtime())
        database.read(0, Long.MAX_VALUE)
            .map { if (active != null && it.id == active.id) active else it }
            .filter { it.endMillis > it.startMillis }
    }

    internal suspend fun mergeBackup(sessions: List<WearSession>): Int = withContext(dispatcher) {
        recorder // Close any interrupted recording before merging history.
        database.merge(sessions).also { if (it > 0) _changes.value++ }
    }

    companion object {
        @Volatile private var instance: WearStatisticsRepository? = null

        fun get(context: Context): WearStatisticsRepository = instance ?: synchronized(this) {
            instance ?: WearStatisticsRepository(context).also { instance = it }
        }
    }
}

private class WearDatabase(context: Context) :
    SQLiteOpenHelper(context, "wear_statistics.db", null, 1), WearSessionStorage {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE sessions (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER,
                last_observed_ms INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX sessions_start ON sessions(start_ms)")
        db.execSQL("CREATE INDEX sessions_end ON sessions(end_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    override fun start(startMillis: Long): Long = writableDatabase.insertOrThrow(
        "sessions", null, ContentValues().apply {
            put("start_ms", startMillis)
            put("last_observed_ms", startMillis)
        }
    )

    override fun checkpoint(id: Long, endMillis: Long) {
        writableDatabase.update("sessions", ContentValues().apply {
            put("last_observed_ms", endMillis)
        }, "_id = ? AND end_ms IS NULL", arrayOf(id.toString()))
    }

    override fun finish(id: Long, endMillis: Long) {
        writableDatabase.update("sessions", ContentValues().apply {
            put("end_ms", endMillis)
            put("last_observed_ms", endMillis)
        }, "_id = ? AND end_ms IS NULL", arrayOf(id.toString()))
    }

    fun merge(sessions: List<WearSession>): Int {
        val db = writableDatabase
        var changed = 0
        db.beginTransaction()
        try {
            for (session in sessions) {
                // Accept a longer snapshot of the same session. Preserve local records
                // if merging would overlap another session or touch the live recorder.
                val overlaps = db.rawQuery(
                    """SELECT * FROM (
                           SELECT _id, start_ms, end_ms FROM sessions WHERE start_ms < ?
                           ORDER BY start_ms DESC LIMIT 1
                       ) WHERE end_ms IS NULL OR end_ms > ?
                       UNION ALL SELECT _id, start_ms, end_ms FROM sessions
                       WHERE start_ms >= ? AND start_ms < ? LIMIT 2""",
                    arrayOf(session.startMillis.toString(), session.startMillis.toString(),
                        session.startMillis.toString(), session.endMillis.toString())
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) add(WearSession(c.getLong(0), c.getLong(1),
                            if (c.isNull(2)) Long.MAX_VALUE else c.getLong(2)))
                    }
                }
                if (overlaps.isEmpty()) {
                    val id = start(session.startMillis)
                    finish(id, session.endMillis)
                    changed++
                } else {
                    val prior = overlaps.singleOrNull() ?: continue
                    if (prior.startMillis == session.startMillis && prior.endMillis < session.endMillis) {
                        val values = ContentValues().apply {
                            put("end_ms", session.endMillis)
                            put("last_observed_ms", session.endMillis)
                        }
                        changed += db.update("sessions", values, "_id = ?", arrayOf(prior.id.toString()))
                    }
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return changed
    }

    fun read(start: Long, end: Long): List<WearSession> = buildList {
        readableDatabase.rawQuery(
            "SELECT _id, start_ms, COALESCE(end_ms, last_observed_ms) FROM sessions " +
                "WHERE start_ms < ? AND (end_ms IS NULL OR end_ms > ?)",
            arrayOf(end.toString(), start.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) add(WearSession(cursor.getLong(0), cursor.getLong(1), cursor.getLong(2)))
        }
    }
}
