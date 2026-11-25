package me.kavishdevar.librepods.presentation.screens

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.data.wear.*
import me.kavishdevar.librepods.presentation.components.MainNavigationBar
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "en-rUS-w392dp-h820dp")
class WearStatisticsUiTest {
    @get:Rule val compose = createComposeRule()
    private val monday = LocalDate.of(2024, 11, 18)
    private fun report(period: WearPeriod): DisplayedWearReport {
        val zone = ZoneId.systemDefault()
        val sessions = (0..6).map { i ->
            val start = monday.plusDays(i.toLong()).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
            WearSession(i.toLong(), start, start + (10 + i) * 60_000L)
        }
        return DisplayedWearReport(period, WearStatistics.report(sessions, period, monday, zone))
    }

    @Test fun chartTouchKeepsWeeklyAndMonthlyReportsAtTheSameScrollPosition() {
        val data = mutableStateOf(report(WearPeriod.WEEK))
        var openedDay: LocalDate? = null
        compose.setContent {
            CompositionLocalProvider(LocalDesignSystem provides DesignSystem.Material) {
                WearStatisticsScreen(data.value.period, data.value, 0, false, {}, {}, { openedDay = it })
            }
        }
        for (period in listOf(WearPeriod.WEEK, WearPeriod.MONTH)) {
            compose.runOnIdle { data.value = report(period) }
            compose.onNodeWithText("Daily details").performScrollTo()
            val chart = compose.onNodeWithText("Wearing time")
            val before = chart.fetchSemanticsNode().positionInRoot
            for (minute in listOf(11, 10, 13)) {
                val bucket = compose.onNode(hasContentDescription(": ${minute} min", substring = true))
                bucket.performTouchInput { click() }
                bucket.assertIsSelected()
                compose.onNode(hasText(": ${minute} min", substring = true)).assertExists()
                assertEquals(before, chart.fetchSemanticsNode().positionInRoot)
                assertNull(openedDay)
            }
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Nov 24, 2024"))
        compose.onNodeWithText("Nov 24, 2024").performClick()
        compose.runOnIdle { assertEquals(monday.plusDays(6), openedDay) }
    }

    @Test fun changingPeriodKeepsTheMeasuredChartWhileTheNextReportLoads() {
        val period = mutableStateOf(WearPeriod.DAY)
        val data = mutableStateOf(report(WearPeriod.DAY))
        compose.setContent {
            CompositionLocalProvider(LocalDesignSystem provides DesignSystem.Apple) {
                WearStatisticsScreen(period.value, data.value, 0, false, { period.value = it }, {}, {})
            }
        }
        val chart = compose.onNodeWithText("Wearing time")
        val before = chart.fetchSemanticsNode().positionInRoot
        compose.onNodeWithText("Week").performClick().assertIsSelected()
        compose.onNodeWithText("Day").assertIsNotSelected()
        compose.onNodeWithText("Daily wearing time").assertExists()
        compose.onNodeWithText("Loading…").assertDoesNotExist()
        compose.runOnIdle { data.value = report(period.value) }
        compose.onNodeWithText("Weekly wearing time").assertExists()
        compose.onNodeWithText("Longest session").assertDoesNotExist()
        compose.onNodeWithText("minutes").assertExists()
        assertEquals(before, chart.fetchSemanticsNode().positionInRoot)
    }

    @Test fun bottomBarHasACompactTouchTargetAndSelectsOneTab() {
        val selected = mutableStateOf(0)
        compose.setContent { MaterialTheme { MainNavigationBar(selected.value) { selected.value = it } } }
        compose.onNodeWithText("Devices").assertIsSelected().assertHeightIsEqualTo(52.dp)
        compose.onNodeWithText("Statistics").performClick().assertIsSelected()
        compose.onNodeWithText("Devices").assertIsNotSelected()
        compose.onNodeWithText("Settings").performClick().assertIsSelected()
        compose.onNodeWithText("Statistics").assertIsNotSelected()
    }
}
