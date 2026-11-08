package me.kavishdevar.librepods.utils;

/** Do not turn unknown or disconnected battery readings into a full battery. */
public final class SystemBatteryPolicy {
    private SystemBatteryPolicy() { }
    public static boolean valid(int level) { return level >= 0 && level <= 100; }
    public static int minimum(int left, int right) {
        if (valid(left) && valid(right)) return Math.min(left, right);
        if (valid(left)) return left;
        return valid(right) ? right : -1;
    }
}
