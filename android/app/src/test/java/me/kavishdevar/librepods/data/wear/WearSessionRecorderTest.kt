package me.kavishdevar.librepods.data.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearSessionRecorderTest {
    private class Store : WearSessionStorage {
        val sessions = mutableListOf<WearSession>()
        val finished = mutableSetOf<Long>()
        override fun insert(startMillis: Long, endMillis: Long, finished: Boolean): Long {
            val id = sessions.size.toLong() + 1
            sessions.add(WearSession(id, startMillis, endMillis))
            if (finished) this.finished.add(id)
            return id
        }
        override fun discard(id: Long) { sessions.removeAll { it.id == id } }
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
        recorder.earState(false, 14_000, 13_100)
        assertEquals(listOf(WearSession(1, 1_000, 14_000)), store.sessions)
        assertEquals(false, recorder.isWearing)
    }

    @Test fun `taking both out and reinserting creates a new session`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.earState(false, 12_000, 11_100)
        recorder.earState(true, 13_000, 12_100)
        recorder.earState(false, 25_000, 24_100)
        assertEquals(2, store.sessions.size)
        assertEquals(2, store.finished.size)
    }

    @Test fun `disconnect and later duplicate stop cannot add time`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.stop(11_100)
        recorder.checkpoint(50_100)
        recorder.stop(100_100)
        assertEquals(WearSession(1, 1_000, 12_000), store.sessions.single())
    }

    @Test fun `wall clock changes do not inflate or negate elapsed duration`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.earState(true, 86_401_000, 1_100)
        recorder.earState(false, -5_000, 12_100)
        assertEquals(WearSession(1, 1_000, 13_000), store.sessions.single())
    }

    @Test fun `checkpoint persists only observed time and does not end session`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.checkpoint(15_100)
        assertEquals(16_000L, store.sessions.single().endMillis)
        assertEquals(true, recorder.isWearing)
        assertEquals(0, store.finished.size)
    }

    @Test fun `stale BLE stop uses last signal rather than timeout detection time`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1_000, 100)
        recorder.checkpoint(15_100)
        recorder.stop(10_100)
        assertEquals(emptyList<WearSession>(), store.sessions)
    }

    @Test fun `no row is created until more than ten seconds have elapsed`() {
        val store = Store()
        val recorder = WearSessionRecorder(store)
        recorder.earState(true, 1000, 0)
        recorder.checkpoint(10_000)
        assertNull(recorder.currentSession(10_000))
        assertEquals(0, store.sessions.size)
        recorder.stop(10_000)
        assertEquals(0, store.sessions.size)
        recorder.earState(true, 20_000, 15_000)
        recorder.stop(25_001)
        assertEquals(listOf(WearSession(1, 20_000, 30_001)), store.sessions)
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
