package me.kavishdevar.librepods.data.wear

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class WearBackupTest {
    @Test fun snapshotRoundTripAndMergeIsIdempotent() = runBlocking {
        val repo = WearStatisticsRepository.get(RuntimeEnvironment.getApplication())
        val session = WearSession(0, 1000, 12_000)
        assertEquals(0, repo.mergeBackup(listOf(WearSession(0, 60_000, 70_000))))
        assertEquals(1, repo.mergeBackup(listOf(session)))
        val file = ByteArrayOutputStream()
        WearBackup.write(file, repo.snapshot())
        val decoded = WearBackup.read(file.toByteArray().inputStream())
        assertEquals(listOf(session), decoded)
        assertEquals(0, repo.mergeBackup(decoded))
        assertEquals(1, repo.mergeBackup(listOf(session.copy(endMillis = 13_000))))
        assertEquals(0, repo.mergeBackup(listOf(session.copy(startMillis = 1500))))
        assertEquals(13_000L, repo.snapshot().single().endMillis)
    }

    @Test fun upgradeDeletesShortRowsAndKeepsTheWholeLongSession() {
        val app = RuntimeEnvironment.getApplication()
        app.openOrCreateDatabase("wear_statistics.db", 0, null).use { db ->
            db.execSQL("CREATE TABLE sessions (_id INTEGER PRIMARY KEY, start_ms INTEGER, end_ms INTEGER, last_observed_ms INTEGER)")
            db.execSQL("INSERT INTO sessions VALUES (1, 1000, 11000, 11000), (2, 20000, NULL, 21000), (3, 40000, NULL, 50001)")
            db.version = 1
        }
        WearDatabase(app).use { helper ->
            val retained = helper.read(0, Long.MAX_VALUE)
            assertEquals(listOf(WearSession(3, 40000, 50001)), retained)
            helper.readableDatabase.rawQuery("SELECT COUNT(*) FROM sessions", null).use { rows ->
                rows.moveToFirst()
                assertEquals(1, rows.getInt(0))
            }
        }
    }

    @Test fun invalidEndAndWrongVersionAreRejected() {
        for (json in listOf("""{"format":"librepods-wear-v1","sessions":[[1,2],[3,2]]}""",
            """{"format":"other","sessions":[]}""")) {
            assertThrows(IllegalArgumentException::class.java) { WearBackup.read(json.byteInputStream()) }
        }
    }
}
