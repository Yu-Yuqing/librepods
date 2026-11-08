package me.kavishdevar.librepods.utils

import org.junit.Assert.*
import org.junit.Test

class PhoneFirstReconnectTest {
    @Test fun macAlreadyConnectedDoesNotBlockPhoneRetry() {
        val p = PhoneFirstReconnect()
        p.opened(1000)
        assertTrue(p.shouldAttempt(1000, true, false, true, false, true))
        assertFalse(p.shouldAttempt(2000, true, false, true, false, true))
        assertTrue(p.shouldAttempt(3300, true, false, false, true, false))
        assertFalse(p.shouldAttempt(6000, true, false, false, true, false))
    }
    @Test fun closingCaseWithBothBudsCancelsSession() {
        val p = PhoneFirstReconnect()
        p.opened(1000)
        assertFalse(p.shouldAttempt(3000, true, false, false, false, true))
        assertFalse(p.shouldAttempt(5000, true, false, true, true, false))
    }
    @Test fun successOrDisabledSettingStopsFurtherAttempts() {
        for (connected in listOf(true, false)) {
            val p = PhoneFirstReconnect()
            p.opened(1000)
            assertFalse(p.shouldAttempt(3000, connected, connected, true, true, false))
            assertFalse(p.shouldAttempt(12000, true, false, true, true, false))
        }
    }
    @Test fun retriesHaveBothCountAndTimeLimits() {
        val p = PhoneFirstReconnect()
        p.opened(1000)
        for (time in listOf(1100L, 3400L))
            assertTrue(p.shouldAttempt(time, true, false, true, true, false))
        assertFalse(p.shouldAttempt(5700, true, false, true, true, false))
        p.opened(50000)
        assertFalse(p.shouldAttempt(55000, true, false, true, true, false))
        p.opened(100000)
        assertTrue(p.shouldAttempt(102000, true, false, true, true, false))
    }
}
