package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

internal data class DisplayedWearReport(val period: WearPeriod, val report: WearReport)

internal class WearScreenModel : ViewModel() {
    var displayed by mutableStateOf<DisplayedWearReport?>(null)
}

@Composable
fun WearStatisticsRoute() {
    val context = LocalContext.current
    val repository = remember(context) { WearStatisticsRepository.get(context) }
    val owner = LocalLifecycleOwner.current
    val model: WearScreenModel = viewModel()
    var periodName by rememberSaveable { mutableStateOf(WearPeriod.DAY.name) }
    var offset by rememberSaveable { mutableIntStateOf(0) }
    var failed by remember { mutableStateOf(false) }
    val selection = WearPeriod.valueOf(periodName)

    LaunchedEffect(selection, offset, repository, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            repository.changes.collectLatest {
                while (isActive) {
                    try {
                        val zone = ZoneId.systemDefault()
                        val date = selection.shift(LocalDate.now(zone), offset)
                        val report = repository.report(selection, date, zone)
                        // Replace the data and its units together, keeping the list measured while loading.
                        model.displayed = DisplayedWearReport(selection, report)
                        failed = false
                    } catch (_: android.database.SQLException) {
                        failed = true
                    } catch (_: java.io.IOException) {
                        failed = true
                    }
                    delay(10_000)
                }
            }
        }
    }
    WearStatisticsScreen(
        selection = selection, displayed = model.displayed, offset = offset, failed = failed,
        onPeriod = { periodName = it.name; offset = 0 },
        onOffset = { offset = it.coerceAtMost(0) },
        onDay = { day ->
            periodName = WearPeriod.DAY.name
            offset = ChronoUnit.DAYS.between(LocalDate.now(), day).toInt().coerceAtMost(0)
        },
    )
}

@Composable
internal fun WearStatisticsScreen(
    selection: WearPeriod,
    displayed: DisplayedWearReport?,
    offset: Int,
    failed: Boolean,
    onPeriod: (WearPeriod) -> Unit,
    onOffset: (Int) -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val period = displayed?.period ?: selection
    val report = displayed?.report
    val apple = LocalDesignSystem.current == DesignSystem.Apple
    val topPadding = if (apple) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp else 16.dp
    val todayLabel = stringResource(R.string.wear_return_current)
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "period") { PeriodPicker(selection, onPeriod) }
        item(key = "dates") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onOffset(offset - 1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.wear_previous_period))
                }
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .clickable(enabled = offset != 0, onClickLabel = todayLabel) { onOffset(0) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(report?.let { dateLabel(it, period) } ?: stringResource(R.string.wear_loading),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    if (offset != 0) Text(todayLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onOffset(offset + 1) }, enabled = offset < 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.wear_next_period))
                }
            }
        }
        if (failed) item(key = "error") {
            Text(stringResource(R.string.wear_load_error), color = MaterialTheme.colorScheme.error)
        }
        if (report == null) {
            if (!failed) item(key = "loading") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            return@LazyColumn
        }
        item(key = "summary") {
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
        }
        item(key = "metrics") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric(stringResource(R.string.wear_session_count), stringResource(R.string.wear_count_value, report.summary.sessionCount), Modifier.weight(1f))
                Metric(stringResource(R.string.wear_average_session), durationText(report.summary.averageMillis), Modifier.weight(1f))
            }
        }
        item(key = "chart") {
            WearCard { WearChart(report, period) }
        }
        if (report.summary.sessionCount == 0) {
            item(key = "empty") {
                Text(stringResource(R.string.wear_empty_title), Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (period == WearPeriod.DAY) {
            item(key = "details") { Text(stringResource(R.string.wear_sessions), style = MaterialTheme.typography.titleMedium) }
            itemsIndexed(report.sessions, key = { _, session -> session.id }) { _, session ->
                val zone = ZoneId.systemDefault()
                val from = report.startDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val to = report.endDate.atStartOfDay(zone).toInstant().toEpochMilli()
                val start = maxOf(session.startMillis, from)
                val end = minOf(session.endMillis, to)
                val active = session.id == report.activeSessionId && session.endMillis <= to
                val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
                val startLabel = Instant.ofEpochMilli(start).atZone(zone).format(timeFormat)
                val endLabel = if (active) stringResource(R.string.wear_ongoing)
                    else Instant.ofEpochMilli(end).atZone(zone).format(timeFormat)
                WearCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("$startLabel – $endLabel")
                            if (session.startMillis < from || session.endMillis > to) {
                                Text(stringResource(R.string.wear_continued_session), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(durationText(end - start), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            item(key = "details") { Text(stringResource(R.string.wear_daily_details), style = MaterialTheme.typography.titleMedium) }
            val days = report.buckets.asReversed().filter { it.summary.sessionCount > 0 }
            itemsIndexed(days, key = { _, bucket -> "day-${bucket.startMillis}" }) { _, bucket ->
                val day = Instant.ofEpochMilli(bucket.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                WearCard(Modifier.clip(RoundedCornerShape(24.dp)).clickable(role = Role.Button) { onDay(day) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
}

@Composable
private fun PeriodPicker(selected: WearPeriod, onSelect: (WearPeriod) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WearPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .selectable(selected = isSelected, interactionSource = null, indication = null, role = Role.Tab) {
                        if (!isSelected) onSelect(period)
                    }.heightIn(min = 44.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(when (period) {
                    WearPeriod.DAY -> R.string.wear_period_day
                    WearPeriod.WEEK -> R.string.wear_period_week
                    WearPeriod.MONTH -> R.string.wear_period_month
                }), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
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
internal fun durationText(millis: Long): String {
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
