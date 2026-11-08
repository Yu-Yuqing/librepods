package me.kavishdevar.librepods.presentation.components

import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.utils.AssistantPermissions

@Composable
fun AssistantPermissionGuide() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var revision by remember { mutableStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) revision++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val background = remember(revision) { AssistantPermissions.check(context, 10021) }
    val lockScreen = remember(revision) { AssistantPermissions.check(context, 10020) }
    val overlay = remember(revision) { Settings.canDrawOverlays(context) }
    val failed = stringResource(R.string.assistant_settings_unavailable)
    fun open(overlayPage: Boolean = false) {
        if (!AssistantPermissions.openSettings(context, overlayPage))
            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
    }
    @Composable fun status(grant: AssistantPermissions.Grant): String = stringResource(
        when (grant) {
            AssistantPermissions.Grant.ALLOWED -> R.string.assistant_permission_allowed
            AssistantPermissions.Grant.REQUIRED -> R.string.assistant_permission_required
            AssistantPermissions.Grant.UNKNOWN -> R.string.assistant_permission_unknown
        }
    )
    StyledList(
        title = stringResource(R.string.assistant_permissions_title),
    ) {
        if (AssistantPermissions.isXiaomi) {
            StyledListItem(
                name = stringResource(R.string.assistant_background_permission),
                description = stringResource(R.string.assistant_background_help),
                orientation = ListItemOrientation.Vertical,
                onClick = { open() },
                trailingContent = { Text(status(background), maxLines = 1, softWrap = false) },
            )
            StyledListItem(
                name = stringResource(R.string.assistant_lockscreen_permission),
                description = stringResource(R.string.assistant_lockscreen_help),
                orientation = ListItemOrientation.Vertical,
                onClick = { open() },
                trailingContent = { Text(status(lockScreen), maxLines = 1, softWrap = false) },
            )
        }
        if (!overlay || !AssistantPermissions.isXiaomi) {
            StyledListItem(
                name = stringResource(R.string.assistant_overlay_permission),
                description = stringResource(R.string.assistant_overlay_help),
                orientation = ListItemOrientation.Vertical,
                onClick = { open(true) },
                trailingContent = { Text(stringResource(if (overlay)
                    R.string.assistant_permission_allowed else R.string.assistant_permission_required),
                    maxLines = 1, softWrap = false) },
            )
        }
    }
}
