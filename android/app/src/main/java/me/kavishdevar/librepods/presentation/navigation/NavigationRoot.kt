package me.kavishdevar.librepods.presentation.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.components.MainNavigationBar
import me.kavishdevar.librepods.presentation.theme.DesignSystem
import me.kavishdevar.librepods.presentation.theme.LocalDesignSystem
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun NavigationRoot(
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    airPodsViewModel: AirPodsViewModel
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var onboarding by rememberSaveable { mutableStateOf(showOnboarding) }
    val tabBackStacks = remember {
        listOf(
            mutableStateListOf<Screen>(Screen.AirPodsSettings),
            mutableStateListOf<Screen>(Screen.AppSettings),
            mutableStateListOf<Screen>(Screen.WearStatistics),
        )
    }
    val onboardingStack = remember { mutableStateListOf<Screen>(Screen.Onboarding) }
    val backStack = if (onboarding) onboardingStack else tabBackStacks[selectedTab]

    BackHandler(enabled = !onboarding && backStack.size == 1 && selectedTab != 0) {
        selectedTab = 0
    }

    val tabState = rememberSaveableStateHolder()
    val currentScreen = backStack.last()

    val deviceState = airPodsViewModel.uiState.collectAsState()
    val deviceTitle by remember(deviceState) { derivedStateOf {
        deviceState.value.run { if (isLocallyConnected) deviceName else null }
    } }
    val trackingActive by remember(deviceState) { derivedStateOf { deviceState.value.headTrackingActive } }

    val m3eEnabled = LocalDesignSystem.current == DesignSystem.Material

    val title = when (currentScreen) {
        Screen.Onboarding -> ""
        Screen.AirPodsSettings -> deviceTitle ?: stringResource(R.string.app_name)
        Screen.Accessibility -> stringResource(R.string.accessibility)
        Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
        Screen.WearStatistics -> stringResource(R.string.wear_statistics)
//        Screen.CameraControl -> stringResource(R.string.camera_control)
        Screen.Equalizer -> stringResource(R.string.equalizer)
        Screen.HeadTracking -> stringResource(R.string.head_tracking)
        Screen.HearingAid -> stringResource(R.string.hearing_aid)
        Screen.HearingAidAdjustments -> stringResource(R.string.adjustments)
        Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> currentScreen.bud
        Screen.OpenSourceLicenses -> stringResource(R.string.open_source_licenses)
        Screen.Purchase -> stringResource(R.string.unlock_advanced_features)
        Screen.Rename -> stringResource(R.string.name)
        Screen.TransparencyCustomization -> stringResource(R.string.customize_transparency_mode)
        Screen.Troubleshooting -> stringResource(R.string.troubleshooting)
        Screen.UpdateHearingTest -> stringResource(R.string.update_hearing_test)
        Screen.VersionInfo -> stringResource(R.string.version)
        is Screen.CallControl -> currentScreen.action
        Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
    }

    val actionButtons = when (currentScreen) {
        Screen.HeadTracking -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
            { scaffoldBackdrop ->
                if (m3eEnabled) {
                    FilledTonalIconToggleButton(
                        checked = trackingActive,
                        onCheckedChange = { if (it) airPodsViewModel.startHeadTracking() else airPodsViewModel.stopHeadTracking() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)),
                        shape = IconButtonDefaults.mediumRoundShape
                    ) {
                        Icon(
                            imageVector = if (trackingActive) MaterialIcons.pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                        )
                    }
                } else {
                    StyledIconButton(
                        onClick = {
                            if (!trackingActive) {
                                airPodsViewModel.startHeadTracking()
                                Log.d("HeadTrackingScreen", "Head tracking started")
                            } else {
                                airPodsViewModel.stopHeadTracking()
                                Log.d("HeadTrackingScreen", "Head tracking stopped")
                            }
                        },
                        icon = if (trackingActive) "􀊅" else "􀊃",
                        backdrop = scaffoldBackdrop
                    )
                }
            }
        )
        else -> listOf()
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            StyledScaffold(
                visible = currentScreen.showTopBar,
                title = title,
                showBackButton = backStack.size > 1,
                onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
                actionButtons = actionButtons,
                hasBottomNavigation = !onboarding,
            ) {
                // Each tab owns its NavDisplay. Tab changes never run a detail-page slide.
                tabState.SaveableStateProvider(if (onboarding) -1 else selectedTab) {
                    AppNavGraph(
                        onboardingComplete = {
                            onboardingComplete()
                            onboarding = false
                        },
                        backStack = backStack,
                        airPodsViewModel = airPodsViewModel,
                    )
                }
            }
        }
        if (!onboarding) MainNavigationBar(selectedTab) { selectedTab = it }
    }
}
