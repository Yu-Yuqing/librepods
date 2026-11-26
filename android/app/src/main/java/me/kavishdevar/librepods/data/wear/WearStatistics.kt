package me.kavishdevar.librepods.data.wear

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

internal const val MIN_WEAR_MILLIS = 10_000L

/** A physical wearing session: either bud in ear, until both are out or tracking stops. */
data class WearSession(val id: Long, val startMillis: Long, val endMillis: Long) {
    val countsAsWear: Boolean get() = endMillis - startMillis > MIN_WEAR_MILLIS
}

enum class WearPeriod {
    DAY, WEEK, MONTH;

    fun startDate(date: LocalDate): LocalDate = when (this) {
        DAY -> date
        WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> date.withDayOfMonth(1)
    }

    fun endDate(start: LocalDate): LocalDate = when (this) {
        DAY -> start.plusDays(1)
        WEEK -> start.plusWeeks(1)
        MONTH -> start.plusMonths(1)
    }

    fun shift(date: LocalDate, offset: Int): LocalDate = when (this) {
        DAY -> date.plusDays(offset.toLong())
        WEEK -> date.plusWeeks(offset.toLong())
        MONTH -> date.plusMonths(offset.toLong())
    }
}

data class WearSummary(
    val totalMillis: Long = 0,
    val sessionCount: Int = 0,
    val longestMillis: Long = 0,
) {
    val averageMillis: Long get() = if (sessionCount == 0) 0 else totalMillis / sessionCount
}

data class WearBucket(val startMillis: Long, val endMillis: Long, val summary: WearSummary)

data class WearReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val summary: WearSummary,
    val buckets: List<WearBucket>,
    val sessions: List<WearSession>,
    val activeSessionId: Long? = null,
    val activeDurationMillis: Long = 0,
)

object WearStatistics {
    fun summarize(sessions: List<WearSession>, start: Long, end: Long): WearSummary {
        var total = 0L
        var count = 0
        var longest = 0L
        for (session in sessions) {
            if (!session.countsAsWear) continue
            val duration = minOf(session.endMillis, end) - maxOf(session.startMillis, start)
            if (duration <= 0) continue
            total += duration
            count++
            longest = maxOf(longest, duration)
        }
        return WearSummary(total, count, longest)
    }

    fun report(
        sessions: List<WearSession>,
        period: WearPeriod,
        date: LocalDate,
        zone: ZoneId,
    ): WearReport {
        val startDate = period.startDate(date)
        val endDate = period.endDate(startDate)
        val start = startDate.atStartOfDay(zone)
        val end = endDate.atStartOfDay(zone)
        val startMillis = start.toInstant().toEpochMilli()
        val endMillis = end.toInstant().toEpochMilli()
        val buckets = buildList {
            var cursor = start
            while (cursor < end) {
                // Calendar boundaries, rather than fixed 24-hour days, also handle DST.
                val next = if (period == WearPeriod.DAY) cursor.plusHours(1)
                    else cursor.toLocalDate().plusDays(1).atStartOfDay(zone)
                val from = cursor.toInstant().toEpochMilli()
                val to = minOf(next.toInstant().toEpochMilli(), endMillis)
                add(WearBucket(from, to, summarize(sessions, from, to)))
                cursor = next
            }
        }
        val overlapping = sessions.filter { it.startMillis < endMillis && it.endMillis > startMillis }
            .sortedByDescending { it.startMillis }
        return WearReport(startDate, endDate, summarize(overlapping, startMillis, endMillis), buckets, overlapping.filter { it.countsAsWear })
    }
}

/** Persistence is supplied by SQLite in the app and an in-memory store in tests. */
interface WearSessionStorage {
    fun insert(startMillis: Long, endMillis: Long, finished: Boolean): Long
    fun discard(id: Long)
    fun checkpoint(id: Long, endMillis: Long)
    fun finish(id: Long, endMillis: Long)
}

/** All calls are serialized by the repository. Durations use the monotonic clock. */
class WearSessionRecorder(private val storage: WearSessionStorage) {
    private var savedId: Long? = null
    private var startMillis = 0L
    private var startElapsed: Long? = null
    val isWearing: Boolean get() = startElapsed != null

    fun earState(wearing: Boolean, wallMillis: Long, elapsedMillis: Long) {
        if (wearing && !isWearing) {
            startMillis = wallMillis
            startElapsed = elapsedMillis
        } else if (!wearing) {
            stop(elapsedMillis)
        }
    }

    fun currentSession(elapsedMillis: Long): WearSession? {
        val duration = elapsedMillis - (startElapsed ?: return null)
        if (duration <= MIN_WEAR_MILLIS) return null
        return WearSession(savedId ?: -1L, startMillis, startMillis + duration)
    }

    fun checkpoint(elapsedMillis: Long) {
        val session = currentSession(elapsedMillis) ?: return
        val id = savedId
        if (id == null) savedId = storage.insert(startMillis, session.endMillis, finished = false)
        else storage.checkpoint(id, session.endMillis)
    }

    fun stop(elapsedMillis: Long) {
        val session = currentSession(elapsedMillis)
        val id = savedId
        if (session != null) {
            if (id == null) storage.insert(startMillis, session.endMillis, finished = true)
            else storage.finish(id, session.endMillis)
        } else if (id != null) {
            // BLE expiry can backdate the end to before the last checkpoint.
            storage.discard(id)
        }
        savedId = null
        startElapsed = null
    }
}
