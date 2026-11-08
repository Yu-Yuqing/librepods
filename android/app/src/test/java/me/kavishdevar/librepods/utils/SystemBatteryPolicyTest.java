package me.kavishdevar.librepods.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SystemBatteryPolicyTest {
    @Test public void usesMinimumOfConnectedBuds() {
        assertEquals(60, SystemBatteryPolicy.minimum(80, 60));
        assertEquals(0, SystemBatteryPolicy.minimum(0, 100));
        assertEquals(100, SystemBatteryPolicy.minimum(100, 100));
    }
    @Test public void singleBudUsesItsActualReading() {
        assertEquals(42, SystemBatteryPolicy.minimum(-1, 42));
        assertEquals(70, SystemBatteryPolicy.minimum(70, -1));
    }
    @Test public void unknownAndInvalidReadingsDoNotBecomeFullBattery() {
        assertEquals(-1, SystemBatteryPolicy.minimum(-1, -1));
        assertEquals(70, SystemBatteryPolicy.minimum(101, 70));
        assertEquals(-1, SystemBatteryPolicy.minimum(-4, 101));
    }
}
