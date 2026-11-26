package me.kavishdevar.librepods.data.wear

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WearStatisticsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun time(value: String, inZone: ZoneId = zone) = ZonedDateTime.parse(value + "[" + inZone.id + "]").toInstant().toEpochMilli()
    private fun session(id: Long, start: String, end: String) = WearSession(id, time(start), time(end))
    private val minute = 60_000L

    @Test fun `day reports total count average and longest`() {
        val sessions = listOf(
            session(1, "2026-11-25T08:00+08:00", "2026-11-25T08:30+08:00"),
            session(2, "2026-11-25T11:00+08:00", "2026-11-25T12:30+08:00"),
        )
        val report = WearStatistics.report(sessions, WearPeriod.DAY, LocalDate.of(2026, 11, 25), zone)
        assertEquals(120 * minute, report.summary.totalMillis)
        assertEquals(2, report.summary.sessionCount)
        assertEquals(60 * minute, report.summary.averageMillis)
        assertEquals(90 * minute, report.summary.longestMillis)
        assertEquals(report.summary.totalMillis, report.buckets.sumOf { it.summary.totalMillis })
    }

    @Test fun `midnight splits duration but week counts original session once`() {
        val sessions = listOf(session(1, "2026-11-24T23:30+08:00", "2026-11-25T00:30+08:00"))
        val day = WearStatistics.report(sessions, WearPeriod.DAY, LocalDate.of(2026, 11, 25), zone)
        val week = WearStatistics.report(sessions, WearPeriod.WEEK, LocalDate.of(2026, 11, 25), zone)
        assertEquals(30 * minute, day.summary.totalMillis)
        assertEquals(30 * minute, day.summary.averageMillis)
        assertEquals(60 * minute, week.summary.totalMillis)
        assertEquals(1, week.summary.sessionCount)
        assertEquals(2, week.buckets.sumOf { it.summary.sessionCount })
        assertEquals(LocalDate.of(2026, 11, 23), week.startDate)
    }

    @Test fun `month clips a session entering from previous month`() {
        val sessions = listOf(session(1, "2026-10-31T23:00+08:00", "2026-11-01T02:00+08:00"))
        val report = WearStatistics.report(sessions, WearPeriod.MONTH, LocalDate.of(2026, 11, 25), zone)
        assertEquals(120 * minute, report.summary.totalMillis)
        assertEquals(120 * minute, report.summary.longestMillis)
        assertEquals(30, report.buckets.size)
    }

    @Test fun `exact boundary and zero length sessions do not add counts`() {
        val sessions = listOf(
            session(1, "2026-11-24T23:30+08:00", "2026-11-25T00:00+08:00"),
            session(2, "2026-11-26T00:00+08:00", "2026-11-26T00:30+08:00"),
            session(3, "2026-11-25T12:00+08:00", "2026-11-25T12:00+08:00"),
        )
        assertEquals(WearSummary(), WearStatistics.report(sessions, WearPeriod.DAY, LocalDate.of(2026, 11, 25), zone).summary)
    }

    @Test fun `full session must exceed thirty seconds even across midnight`() {
        val date = LocalDate.of(2026, 11, 25)
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = listOf(WearSession(1, midnight - 30_000, midnight + 1),
            WearSession(2, midnight + 1000, midnight + 31_000),
            WearSession(3, midnight + 40_000, midnight + 41_000))
        for (period in WearPeriod.entries) {
            val report = WearStatistics.report(sessions, period, date, zone)
            val counted = if (period == WearPeriod.DAY) 1L else 30_001L
            assertEquals(1, report.summary.sessionCount)
            assertEquals(counted, report.summary.averageMillis)
            assertEquals(counted + 31_000, report.summary.totalMillis)
            assertEquals(listOf(1L), report.sessions.map { it.id })
        }
    }

    @Test fun `empty history has zero average and complete calendar buckets`() {
        val report = WearStatistics.report(emptyList(), WearPeriod.MONTH, LocalDate.of(2028, 2, 10), zone)
        assertEquals(0L, report.summary.averageMillis)
        assertEquals(29, report.buckets.size)
        assertEquals(LocalDate.of(2028, 3, 1), report.endDate)
    }

    @Test fun `spring DST day has 23 hourly buckets without invented time`() {
        val dstZone = ZoneId.of("America/New_York")
        val start = time("2026-03-08T00:00-05:00", dstZone)
        val end = time("2026-03-09T00:00-04:00", dstZone)
        val report = WearStatistics.report(listOf(WearSession(1, start, end)), WearPeriod.DAY, LocalDate.of(2026, 3, 8), dstZone)
        assertEquals(23, report.buckets.size)
        assertEquals(23 * 60 * minute, report.summary.totalMillis)
        assertEquals(report.summary.totalMillis, report.buckets.sumOf { it.summary.totalMillis })
    }

    @Test fun `fall DST day has 25 hourly buckets and retains repeated hour`() {
        val dstZone = ZoneId.of("America/New_York")
        val start = time("2026-11-01T00:00-04:00", dstZone)
        val end = time("2026-11-02T00:00-05:00", dstZone)
        val report = WearStatistics.report(listOf(WearSession(1, start, end)), WearPeriod.DAY, LocalDate.of(2026, 11, 1), dstZone)
        assertEquals(25, report.buckets.size)
        assertEquals(25 * 60 * minute, report.summary.totalMillis)
        assertEquals(report.summary.totalMillis, report.buckets.sumOf { it.summary.totalMillis })
    }

    @Test fun `week and month offsets work across years`() {
        val date = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2025, 12, 29), WearPeriod.WEEK.startDate(date))
        assertEquals(LocalDate.of(2025, 12, 1), WearPeriod.MONTH.shift(date, -1))
    }

    @Test fun `timezone determines the local day of a saved absolute interval`() {
        val sessions = listOf(session(1, "2026-11-25T00:00+08:00", "2026-11-25T01:00+08:00"))
        val utc = WearStatistics.report(sessions, WearPeriod.DAY, LocalDate.of(2026, 11, 24), ZoneId.of("UTC"))
        assertEquals(60 * minute, utc.summary.totalMillis)
    }
}
