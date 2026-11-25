package me.kavishdevar.librepods.presentation.screens

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.wear.WearPeriod
import me.kavishdevar.librepods.data.wear.WearReport
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val INTERVALS = 4
private val tickSteps = longArrayOf(1_000, 5_000, 15_000, 30_000, MINUTE, 2 * MINUTE, 5 * MINUTE,
    10 * MINUTE, 15 * MINUTE, 30 * MINUTE, HOUR, 2 * HOUR, 3 * HOUR, 6 * HOUR, 8 * HOUR, 12 * HOUR)

internal fun wearChartTickMillis(peak: Long, period: WearPeriod): Long {
    if (peak <= 0) return if (period == WearPeriod.DAY) 15 * MINUTE else HOUR
    return tickSteps.firstOrNull { it * INTERVALS >= peak }
        ?: ((peak - 1) / (HOUR * INTERVALS) + 1) * HOUR
}

@Composable
internal fun WearChart(report: WearReport, period: WearPeriod) {
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall
    val zone = ZoneId.systemDefault()
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, period) {
        if (period == WearPeriod.DAY) DateTimeFormatter.ofPattern("HH:mm", locale)
        else DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMEd"), locale)
    }
    val buckets = report.buckets
    if (buckets.isEmpty()) return
    val dates = remember(report.startDate, period, zone) {
        buckets.map { Instant.ofEpochMilli(it.startMillis).atZone(zone) }
    }
    val peak = buckets.maxOf { it.summary.totalMillis }
    val step = wearChartTickMillis(peak, period)
    val unit = when {
        step % HOUR == 0L -> HOUR
        step % MINUTE == 0L -> MINUTE
        else -> 1_000L
    }
    val unitLabel = stringResource(when (unit) {
        HOUR -> R.string.wear_axis_hours
        MINUTE -> R.string.wear_axis_minutes
        else -> R.string.wear_axis_seconds
    })
    var picked by rememberSaveable(period.name, report.startDate.toEpochDay()) { mutableIntStateOf(-1) }
    val now = System.currentTimeMillis()
    val lastAvailable = buckets.indexOfLast { it.startMillis <= now }
    val selected = if (picked in 0..lastAvailable) picked
        else buckets.indexOfLast { it.summary.totalMillis > 0L }.takeIf { it >= 0 } ?: lastAvailable
    val readout = buckets.getOrNull(selected)?.let {
        stringResource(R.string.wear_chart_description, dates[selected].format(formatter), durationText(it.summary.totalMillis))
    }.orEmpty()
    val tickHeight = with(LocalDensity.current) { labelStyle.lineHeight.toDp() }
    val plotHeight = 128.dp
    val axisWidth = with(LocalDensity.current) { maxOf(28.dp, labelStyle.fontSize.toDp() * 3.2f) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val gridColor = colors.outlineVariant
    Text(stringResource(R.string.wear_duration_chart), style = MaterialTheme.typography.titleMedium)
    Text(readout, Modifier.fillMaxWidth().padding(top = 4.dp), maxLines = 1, minLines = 1,
        overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = colors.primary)
    Text(unitLabel, Modifier.padding(top = 8.dp), style = labelStyle, color = colors.onSurfaceVariant)
    Row(Modifier.fillMaxWidth().height(plotHeight + tickHeight)) {
        Column(Modifier.width(axisWidth).fillMaxHeight().padding(end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) {
            for (i in INTERVALS downTo 0) {
                Text((step * i / unit).toString(), style = labelStyle, color = colors.onSurfaceVariant, maxLines = 1)
            }
        }
        Layout(
            modifier = Modifier.weight(1f).padding(vertical = tickHeight / 2).fillMaxHeight()
                .drawBehind {
                    for (i in 0..INTERVALS) {
                        val y = size.height * i / INTERVALS
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                }
                .pointerInput(report.startDate, period, buckets.size, lastAvailable, rtl) {
                    detectTapGestures { point ->
                        val slot = (point.x / size.width * buckets.size).toInt().coerceIn(buckets.indices)
                        val index = if (rtl) buckets.lastIndex - slot else slot
                        if (index <= lastAvailable) picked = index
                    }
                },
            content = {
                buckets.forEachIndexed { index, bucket ->
                    val chosen = index == selected
                    val description = stringResource(R.string.wear_chart_description, dates[index].format(formatter),
                        durationText(bucket.summary.totalMillis))
                    Box(Modifier.fillMaxSize()
                        .then(if (chosen) Modifier.background(colors.primary.copy(alpha = 0.08f)) else Modifier)
                        .semantics {
                            contentDescription = description
                            this.selected = chosen
                            if (index <= lastAvailable) onClick { picked = index; true }
                        }, contentAlignment = Alignment.BottomCenter) {
                        if (bucket.summary.totalMillis > 0) {
                            val fraction = bucket.summary.totalMillis.toFloat() / (step * INTERVALS)
                            Box(Modifier.fillMaxWidth(0.68f).fillMaxHeight(fraction.coerceIn(0f, 1f))
                                .background(if (chosen) colors.primary else colors.primary.copy(alpha = 0.54f),
                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            // The same fractional boundaries drive drawing, touch and tick positions.
            val edges = IntArray(measurables.size + 1) { i ->
                (i.toFloat() * width / measurables.size).roundToInt()
            }
            val bars = measurables.mapIndexed { i, bar ->
                bar.measure(Constraints.fixed(edges[i + 1] - edges[i], height))
            }
            layout(width, height) {
                bars.forEachIndexed { i, bar -> bar.placeRelative(edges[i], 0) }
            }
        }
    }
    val weekdayFormat = remember(locale) { DateTimeFormatter.ofPattern("EEEEE", locale) }
    val indices = remember(period, dates) {
        when (period) {
            WearPeriod.DAY -> dates.indices.filter { dates[it].hour in listOf(0, 6, 12, 18) }.distinctBy { dates[it].hour }
            WearPeriod.WEEK -> dates.indices.toList()
            WearPeriod.MONTH -> listOf(0, 7, 14, 21, dates.lastIndex).distinct()
        }
    }
    Layout(
        modifier = Modifier.fillMaxWidth().padding(start = axisWidth),
        content = {
            indices.forEach { i ->
                val label = when (period) {
                    WearPeriod.DAY -> "%02d".format(locale, dates[i].hour)
                    WearPeriod.WEEK -> dates[i].format(weekdayFormat)
                    WearPeriod.MONTH -> dates[i].dayOfMonth.toString()
                }
                Text(label, style = labelStyle, color = colors.onSurfaceVariant, maxLines = 1)
            }
        },
    ) { measurables, constraints ->
        val labels = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        layout(width, labels.maxOfOrNull { it.height } ?: 0) {
            labels.forEachIndexed { n, label ->
                val center = (indices[n] + 0.5f) * width / buckets.size
                label.placeRelative((center - label.width / 2f).roundToInt().coerceIn(0, width - label.width), 0)
            }
        }
    }
}
