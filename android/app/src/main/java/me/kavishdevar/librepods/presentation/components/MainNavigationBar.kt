package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem

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
    val apple = LocalDesignSystem.current == DesignSystem.Apple
    val labels = listOf(R.string.nav_devices, R.string.settings, R.string.wear_statistics)
    val icons = listOf(headphonesIcon, Icons.Outlined.Settings, statisticsIcon)
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = if (apple) 0.dp else 3.dp,
    ) {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onSelect(index) },
                icon = { Icon(icons[index], null, Modifier.size(24.dp)) },
                label = { Text(stringResource(label), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = if (apple) 0.10f else 0.16f),
                ),
            )
        }
    }
}
