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
    private fun report(period: WearPeriod): WearReport {
        val zone = ZoneId.systemDefault()
        val sessions = (0..6).map { i ->
            val start = monday.plusDays(i.toLong()).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
            WearSession(i.toLong(), start, start + (10 + i) * 60_000L)
        }
        return WearStatistics.report(sessions, period, monday, zone)
    }

    @Test fun chartTouchKeepsWeeklyAndMonthlyReportsAtTheSameScrollPosition() {
        val current = mutableStateOf(WearPeriod.WEEK)
        var openedDay: LocalDate? = null
        compose.setContent {
            CompositionLocalProvider(LocalDesignSystem provides DesignSystem.Material) {
                WearPeriodPage(current.value, report(current.value), 0, false, {}, { openedDay = it })
            }
        }
        for (period in listOf(WearPeriod.WEEK, WearPeriod.MONTH)) {
            compose.runOnIdle { current.value = period }
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

    @Test fun horizontalSwipesSelectAdjacentPeriodsAndStopAtTheEnds() {
        compose.setContent {
            WearStatisticsPager { period, _ -> WearPeriodPage(period, report(period), 0, false, {}, {}) }
        }
        val list = compose.onNodeWithTag("wearPager")
        for (label in listOf("Week", "Month", "Month")) {
            list.performTouchInput { swipeLeft() }
            compose.onNodeWithText(label).assertIsSelected()
        }
        list.performTouchInput { swipeRight() }
        compose.onNodeWithText("Week").assertIsSelected()
    }

    @Test fun indicatorMovesWhileFingerIsDownAndSettlesOnTap() {
        compose.setContent {
            WearStatisticsPager { period, _ -> WearPeriodPage(period, report(period), 0, false, {}, {}) }
        }
        val indicator = compose.onNodeWithTag("wearIndicator", useUnmergedTree = true)
        val before = indicator.fetchSemanticsNode().boundsInRoot
        val pager = compose.onNodeWithTag("wearPager")
        pager.performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-width * 0.12f, 0f))
            moveBy(androidx.compose.ui.geometry.Offset(-width * 0.16f, 0f))
        }
        val dragged = indicator.fetchSemanticsNode().boundsInRoot.left - before.left
        assertTrue("Indicator offset during drag: $dragged", dragged > 0f && dragged < before.width)
        pager.performTouchInput { up() }
        compose.onNodeWithText("Month").performClick().assertIsSelected()
        compose.onNodeWithText("Monthly wearing time").assertIsDisplayed()
        compose.onNodeWithText("Longest session").assertDoesNotExist()
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
