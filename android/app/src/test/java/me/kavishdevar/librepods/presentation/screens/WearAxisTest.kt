package me.kavishdevar.librepods.presentation.screens

import me.kavishdevar.librepods.data.wear.WearPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WearAxisTest {
    @Test fun scaleCoversShortSessionsFullHoursAndLongDays() {
        assertEquals(1_000L, wearChartTickMillis(1_200L, WearPeriod.DAY))
        assertEquals(60_000L, wearChartTickMillis(160_000L, WearPeriod.WEEK))
        assertEquals(15 * 60_000L, wearChartTickMillis(3_600_000L, WearPeriod.DAY))
        assertEquals(6 * 3_600_000L, wearChartTickMillis(24 * 3_600_000L, WearPeriod.WEEK))
        assertTrue(4 * wearChartTickMillis(25 * 3_600_000L, WearPeriod.MONTH) >= 25 * 3_600_000L)
        assertTrue(wearChartTickMillis(0L, WearPeriod.MONTH) > 0)
    }
}
