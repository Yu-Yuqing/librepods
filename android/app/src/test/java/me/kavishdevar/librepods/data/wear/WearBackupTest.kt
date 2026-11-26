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
        val session = WearSession(0, 1000, 2000)
        assertEquals(1, repo.mergeBackup(listOf(session)))
        val file = ByteArrayOutputStream()
        WearBackup.write(file, repo.snapshot())
        val decoded = WearBackup.read(file.toByteArray().inputStream())
        assertEquals(listOf(session), decoded)
        assertEquals(0, repo.mergeBackup(decoded))
        assertEquals(1, repo.mergeBackup(listOf(session.copy(endMillis = 3000))))
        assertEquals(0, repo.mergeBackup(listOf(session.copy(startMillis = 1500))))
        assertEquals(3000L, repo.snapshot().single().endMillis)
    }

    @Test fun invalidEndAndWrongVersionAreRejected() {
        for (json in listOf("""{"format":"librepods-wear-v1","sessions":[[1,2],[3,2]]}""",
            """{"format":"other","sessions":[]}""")) {
            assertThrows(IllegalArgumentException::class.java) { WearBackup.read(json.byteInputStream()) }
        }
    }
}
