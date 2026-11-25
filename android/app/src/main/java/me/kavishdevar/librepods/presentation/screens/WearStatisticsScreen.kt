package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.wear.WearPeriod
import me.kavishdevar.librepods.data.wear.WearReport
import me.kavishdevar.librepods.data.wear.WearStatisticsRepository
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun WearStatisticsRoute() {
    val context = LocalContext.current
    val repository = remember { WearStatisticsRepository.get(context) }
    val owner = LocalLifecycleOwner.current
    var periodName by rememberSaveable { mutableStateOf(WearPeriod.DAY.name) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    val period = WearPeriod.valueOf(periodName)
    val report by produceState<WearReport?>(null, period, offset, owner) {
        value = null
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            repository.changes.collectLatest {
                while (isActive) {
                    try {
                        value = repository.report(period, period.shift(LocalDate.now(), offset), ZoneId.systemDefault())
                        failed = false
                    } catch (_: java.io.IOException) {
                        failed = true
                    } catch (_: android.database.SQLException) {
                        failed = true
                    }
                    delay(10_000)
                }
            }
        }
    }

    WearStatisticsScreen(
        period = period,
        offset = offset,
        report = report,
        failed = failed,
        onPeriod = { periodName = it.name; offset = 0 },
        onOffset = { offset = it.coerceAtMost(0) },
        onDay = { day ->
            periodName = WearPeriod.DAY.name
            offset = ChronoUnit.DAYS.between(LocalDate.now(), day).toInt().coerceAtMost(0)
        },
    )
}

@Composable
fun WearStatisticsScreen(
    period: WearPeriod,
    offset: Int,
    report: WearReport?,
    failed: Boolean,
    onPeriod: (WearPeriod) -> Unit,
    onOffset: (Int) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val apple = LocalDesignSystem.current == DesignSystem.Apple
    val topPadding = if (apple) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp else 16.dp
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(topPadding))
        PeriodPicker(period, onPeriod)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onOffset(offset - 1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.wear_previous_period))
            }
            Text(
                report?.let { dateLabel(it, period) } ?: stringResource(R.string.wear_loading),
                Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { onOffset(offset + 1) }, enabled = offset < 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.wear_next_period))
            }
        }
        if (offset != 0) {
            TextButton(onClick = { onOffset(0) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.wear_return_current))
            }
        }
        if (failed) {
            Text(stringResource(R.string.wear_load_error), color = MaterialTheme.colorScheme.error)
        } else if (report == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        if (report != null) {
            WearCard {
                Text(stringResource(when (period) {
                    WearPeriod.DAY -> R.string.wear_total_day
                    WearPeriod.WEEK -> R.string.wear_total_week
                    WearPeriod.MONTH -> R.string.wear_total_month
                }), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                Text(
                    durationText(report.summary.totalMillis),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                )
                if (report.activeSessionId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                        Text(
                            stringResource(R.string.wear_current_session, durationText(report.activeDurationMillis)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                } else {
                    Text(stringResource(R.string.wear_not_wearing), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(stringResource(R.string.wear_session_count), stringResource(R.string.wear_count_value, report.summary.sessionCount), Modifier.weight(1f))
                Metric(stringResource(R.string.wear_average_session), durationText(report.summary.averageMillis), Modifier.weight(1f))
            }
            Metric(stringResource(R.string.wear_longest_session), durationText(report.summary.longestMillis), Modifier.fillMaxWidth())

            WearCard {
                Text(stringResource(R.string.wear_duration_chart), style = MaterialTheme.typography.titleMedium)
                WearChart(report, period, onDay)
            }
            if (report.summary.sessionCount == 0) {
                WearCard {
                    Text(stringResource(R.string.wear_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.wear_empty_description), Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (period == WearPeriod.DAY) {
                SessionList(report)
            } else {
                WearCard {
                    Text(stringResource(R.string.wear_daily_details), style = MaterialTheme.typography.titleMedium)
                    report.buckets.asReversed().filter { it.summary.sessionCount > 0 }.forEach { bucket ->
                        val day = Instant.ofEpochMilli(bucket.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable(role = Role.Button) { onDay(day) }.padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
                                Text(stringResource(R.string.wear_count_value, bucket.summary.sessionCount),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(durationText(bucket.summary.totalMillis), fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                        }
                    }
                }
            }
        }
        Text(stringResource(R.string.wear_recording_note), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PeriodPicker(selected: WearPeriod, onSelect: (WearPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WearPeriod.entries.forEach { period ->
            Surface(
                selected = period == selected,
                onClick = { onSelect(period) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = if (period == selected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent,
                contentColor = if (period == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Text(stringResource(when (period) {
                    WearPeriod.DAY -> R.string.wear_period_day
                    WearPeriod.WEEK -> R.string.wear_period_week
                    WearPeriod.MONTH -> R.string.wear_period_month
                }), Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WearCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    WearCard(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WearChart(report: WearReport, period: WearPeriod, onDay: (LocalDate) -> Unit) {
    val maxDuration = (report.buckets.maxOfOrNull { it.summary.totalMillis } ?: 0).coerceAtLeast(1)
    val zone = ZoneId.systemDefault()
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp).height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(if (period == WearPeriod.WEEK) 8.dp else 3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        report.buckets.forEach { bucket ->
            val date = Instant.ofEpochMilli(bucket.startMillis).atZone(zone)
            val description = stringResource(R.string.wear_chart_description,
                date.format(DateTimeFormatter.ofPattern(if (period == WearPeriod.DAY) "HH:mm" else "MMM d")),
                durationText(bucket.summary.totalMillis))
            Box(
                Modifier.weight(1f).height(120.dp).semantics { contentDescription = description }
                    .then(if (period != WearPeriod.DAY && !date.toLocalDate().isAfter(LocalDate.now()))
                        Modifier.clickable(role = Role.Button) { onDay(date.toLocalDate()) } else Modifier),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(Modifier.fillMaxWidth().height(
                    (112f * bucket.summary.totalMillis.toFloat() / maxDuration).coerceAtLeast(3f).dp
                ).clip(RoundedCornerShape(5.dp)).background(
                    if (bucket.summary.totalMillis > 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ))
            }
        }
    }
    val ticks = report.buckets.mapIndexedNotNull { index, bucket ->
        val date = Instant.ofEpochMilli(bucket.startMillis).atZone(zone)
        val label = when (period) {
            WearPeriod.DAY -> if (date.hour % 6 == 0) date.hour.toString() else null
            WearPeriod.WEEK -> date.format(DateTimeFormatter.ofPattern("EE", Locale.getDefault()))
            WearPeriod.MONTH -> if (date.dayOfMonth == 1 || date.dayOfMonth % 7 == 0 || index == report.buckets.lastIndex)
                date.dayOfMonth.toString() else null
        }
        label?.let { index to it }
    }
    Layout(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        content = {
            ticks.forEach { (_, label) ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    ) { measurables, constraints ->
        val labels = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val height = labels.maxOfOrNull { it.height } ?: 0
        val gap = (if (period == WearPeriod.WEEK) 8.dp else 3.dp).roundToPx()
        val barWidth = (width - gap * (report.buckets.size - 1)).toFloat() / report.buckets.size
        layout(width, height) {
            labels.forEachIndexed { index, label ->
                val center = ticks[index].first * (barWidth + gap) + barWidth / 2
                label.placeRelative((center - label.width / 2).toInt().coerceIn(0, (width - label.width).coerceAtLeast(0)), 0)
            }
        }
    }
}

@Composable
private fun SessionList(report: WearReport) {
    val zone = ZoneId.systemDefault()
    val from = report.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val to = report.endDate.atStartOfDay(zone).toInstant().toEpochMilli()
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    WearCard {
        Text(stringResource(R.string.wear_sessions), style = MaterialTheme.typography.titleMedium)
        report.sessions.forEachIndexed { index, session ->
            val start = maxOf(session.startMillis, from)
            val end = minOf(session.endMillis, to)
            val active = session.id == report.activeSessionId && session.endMillis <= to
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Instant.ofEpochMilli(start).atZone(zone).format(timeFormat) + " – " +
                            if (active) stringResource(R.string.wear_ongoing) else Instant.ofEpochMilli(end).atZone(zone).format(timeFormat)
                    )
                    if (session.startMillis < from || session.endMillis > to) {
                        Text(stringResource(R.string.wear_continued_session), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(durationText(end - start), fontWeight = FontWeight.SemiBold)
            }
            if (index < report.sessions.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }
    }
}

@Composable
private fun durationText(millis: Long): String {
    val seconds = millis.coerceAtLeast(0) / 1_000
    return when {
        seconds >= 3_600 -> stringResource(R.string.wear_duration_hours_minutes, seconds / 3_600, seconds % 3_600 / 60)
        seconds >= 60 -> stringResource(R.string.wear_duration_minutes, seconds / 60)
        else -> stringResource(R.string.wear_duration_seconds, seconds)
    }
}

private fun dateLabel(report: WearReport, period: WearPeriod): String = when (period) {
    WearPeriod.DAY -> report.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    WearPeriod.WEEK -> report.startDate.format(DateTimeFormatter.ofPattern("MMM d")) + " – " +
        report.endDate.minusDays(1).format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    WearPeriod.MONTH -> report.startDate.format(DateTimeFormatter.ofPattern("yyyy MMMM"))
}
