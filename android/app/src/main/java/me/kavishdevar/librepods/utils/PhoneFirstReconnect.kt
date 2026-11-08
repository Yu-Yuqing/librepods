package me.kavishdevar.librepods.utils

/** One bounded reconnect window per case-opening session, regardless of remote ownership. */
class PhoneFirstReconnect {
    private var deadline = 0L
    private var attempts = 0
    private var nextAttempt = 0L

    fun opened(now: Long) {
        deadline = now + 5_000L
        attempts = 0
        nextAttempt = now
    }

    fun stop() { deadline = 0L }

    fun deadlineMs(): Long = deadline

    fun isActive(now: Long): Boolean = deadline != 0L && now < deadline && attempts < 2

    fun shouldAttempt(now: Long, enabled: Boolean, connected: Boolean,
                      lidOpen: Boolean, wearing: Boolean, bothCharging: Boolean): Boolean {
        if (!enabled || connected || (!lidOpen && bothCharging && !wearing)) {
            stop()
            return false
        }
        if (!isActive(now) || now < nextAttempt || (!lidOpen && !wearing)) return false
        attempts++
        nextAttempt = now + 2_200L
        return true
    }
}
