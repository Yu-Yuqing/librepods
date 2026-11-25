package me.kavishdevar.librepods.data.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearSessionRecorderTest {
    private class Store : WearSessionStorage {
        val sessions = mutableListOf<WearSession>()
        val finished = mutableSetOf<Long>()
        override fun start(startMillis: Long): Long {
            val id = sessions.size.toLong() + 1
            sessions.add(WearSession(id, startMillis, startMillis))
            return id
        }
        override fun checkpoint(id: Long, endMillis: Long) {
            val index = sessions.indexOfFirst { it.id == id }
            sessions[index] = sessions[index].copy(endMillis = endMillis)
        }
        override fun finish(id: Long, endMillis: Long) { checkpoint(id, endMillis); finished.add(id) }
    }

    @Test fun `repeated reports and changing between one and two buds keep one session`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.earState(true, 2_000, 1_100)
        recorder.earState(true, 3_000, 2_100)
        recorder.earState(false, 4_000, 3_100)
        assertEquals(listOf(WearSession(1, 1_000, 4_000)), store.sessions)
        assertNull(recorder.activeId)
    }

    @Test fun `taking both out and reinserting creates a new session`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.earState(false, 2_000, 1_100)
        recorder.earState(true, 3_000, 2_100)
        recorder.earState(false, 5_000, 4_100)
        assertEquals(2, store.sessions.size)
        assertEquals(2, store.finished.size)
    }

    @Test fun `disconnect and later duplicate stop cannot add time`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.stop(1_100)
        recorder.checkpoint(50_100)
        recorder.stop(100_100)
        assertEquals(WearSession(1, 1_000, 2_000), store.sessions.single())
    }

    @Test fun `wall clock changes do not inflate or negate elapsed duration`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.earState(true, 86_401_000, 1_100)
        recorder.earState(false, -5_000, 2_100)
        assertEquals(WearSession(1, 1_000, 3_000), store.sessions.single())
    }

    @Test fun `checkpoint persists only observed time and does not end session`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.checkpoint(15_100)
        assertEquals(16_000L, store.sessions.single().endMillis)
        assertEquals(1L, recorder.activeId)
        assertEquals(0, store.finished.size)
    }

    @Test fun `stale BLE stop uses last signal rather than timeout detection time`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.checkpoint(15_100)
        recorder.stop(10_100)
        assertEquals(WearSession(1, 1_000, 11_000), store.sessions.single())
    }

    @Test fun `out of ear events and an idle connection do not create history`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(false, 1_000, 100)
        recorder.checkpoint(15_100)
        recorder.stop(20_100)
        assertEquals(0, store.sessions.size)
    }
}
