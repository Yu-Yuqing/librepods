package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R

private val headphonesIcon = ImageVector.Builder("Headphones", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 3f)
        curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
        verticalLineTo(19f)
        curveTo(3f, 20.1f, 3.9f, 21f, 5f, 21f)
        horizontalLineTo(8f)
        verticalLineTo(13f)
        horizontalLineTo(5f)
        verticalLineTo(12f)
        curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
        curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
        verticalLineTo(13f)
        horizontalLineTo(16f)
        verticalLineTo(21f)
        horizontalLineTo(19f)
        curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
        verticalLineTo(12f)
        curveTo(21f, 7.03f, 16.97f, 3f, 12f, 3f)
        close()
    }
}.build()

private val statisticsIcon = ImageVector.Builder("Statistics", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 13f); horizontalLineTo(8f); verticalLineTo(21f); horizontalLineTo(4f); close()
        moveTo(10f, 3f); horizontalLineTo(14f); verticalLineTo(21f); horizontalLineTo(10f); close()
        moveTo(16f, 8f); horizontalLineTo(20f); verticalLineTo(21f); horizontalLineTo(16f); close()
    }
}.build()

@Composable
fun MainNavigationBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val labels = listOf(R.string.nav_devices, R.string.settings, R.string.wear_statistics)
    val icons = listOf(headphonesIcon, Icons.Outlined.Settings, statisticsIcon)
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall
    val itemHeight = with(LocalDensity.current) { maxOf(52.dp, 31.dp + labelStyle.lineHeight.toDp()) }
    Surface(color = colors.surface) {
        Row(
            Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing.only(
                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
            )).selectableGroup(),
        ) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val tint = if (selected) colors.primary else colors.onSurfaceVariant
                Column(
                    Modifier.weight(1f).height(itemHeight)
                        .selectable(selected, role = Role.Tab) { if (!selected) onSelect(index) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Icon(icons[index], null, Modifier.size(21.dp), tint = tint)
                    Text(stringResource(label), color = tint, style = labelStyle, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
