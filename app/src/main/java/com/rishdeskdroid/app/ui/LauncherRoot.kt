package com.rishdeskdroid.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.StatusState
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.openBatterySettings
import com.rishdeskdroid.app.data.openBluetoothSettings
import com.rishdeskdroid.app.data.openWifiSettings

enum class Screen { Home, Drawer, Settings }

/** Wallpaper, the three screens, and the status bar that sits on top of all of them. */
@Composable
fun LauncherRoot(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    widgets: WidgetController,
    status: StatusState,
    screen: Screen,
    otgConnected: Boolean,
    focusSearch: Boolean,
    onSearchFocusHandled: () -> Unit,
    onScreenChange: (Screen) -> Unit,
) {
    val context = LocalContext.current
    var homeEdit by rememberSaveable { mutableStateOf(false) }

    // Leaving the home screen ends layout editing
    LaunchedEffect(screen) {
        if (screen != Screen.Home) homeEdit = false
    }

    // Back always returns toward Home and never closes the launcher. Overlays on each screen
    // register their own handlers, which take priority over this one.
    BackHandler(enabled = true) {
        if (screen != Screen.Home) onScreenChange(Screen.Home)
    }

    // Interactive content stays inside the system bars (gesture bar or three-button bar, on whichever
    // edge it sits), display cutouts and the system gesture strip. The wallpaper still fills the screen.
    // The keyboard is not included, so opening it never squeezes the home screen.
    val usable = WindowInsets.systemBars
        .union(WindowInsets.displayCutout)
        .union(WindowInsets.mandatorySystemGestures.only(WindowInsetsSides.Bottom))
    val sideAndBottom = usable.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
    val sides = usable.only(WindowInsetsSides.Horizontal)

    val animated = prefs.animations
    val fadeEnter: EnterTransition = if (animated) fadeIn() else EnterTransition.None
    val fadeExit: ExitTransition = if (animated) fadeOut() else ExitTransition.None
    val slideEnter: EnterTransition = if (animated) fadeIn() + slideInVertically { it / 10 } else EnterTransition.None
    val slideExit: ExitTransition = if (animated) fadeOut() + slideOutVertically { it / 10 } else ExitTransition.None

    // With Android's own status bar showing, the custom one is hidden to avoid showing two
    val topPadding = if (prefs.showSystemStatusBar) {
        Modifier.windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
    } else {
        Modifier.padding(top = StatusBarHeight)
    }

    CompositionLocalProvider(
        LocalAccent provides Color(prefs.accent),
        LocalAnimations provides animated,
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xFF14163A))) {
            Wallpaper(prefs, blurred = screen != Screen.Home)

            // Dim the blurred wallpaper behind the drawer and settings
            AnimatedVisibility(visible = screen != Screen.Home, enter = fadeEnter, exit = fadeExit) {
                Box(Modifier.fillMaxSize().background(Color(0xFF0A0C20).copy(alpha = 0.5f)))
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(sideAndBottom)
                    .then(topPadding),
            ) {
                AnimatedVisibility(visible = screen == Screen.Home, enter = fadeEnter, exit = fadeExit) {
                    HomeScreen(
                        prefs = prefs,
                        apps = apps,
                        widgets = widgets,
                        otgConnected = otgConnected,
                        editMode = homeEdit,
                        onEditModeChange = { homeEdit = it },
                        onOpenDrawer = { onScreenChange(Screen.Drawer) },
                        onOpenSettings = { onScreenChange(Screen.Settings) },
                    )
                }
                AnimatedVisibility(visible = screen == Screen.Drawer, enter = slideEnter, exit = slideExit) {
                    DrawerScreen(
                        prefs = prefs,
                        apps = apps,
                        focusSearch = focusSearch,
                        onSearchFocusHandled = onSearchFocusHandled,
                        onClose = { onScreenChange(Screen.Home) },
                    )
                }
                AnimatedVisibility(visible = screen == Screen.Settings, enter = slideEnter, exit = slideExit) {
                    SettingsScreen(
                        prefs = prefs,
                        widgets = widgets,
                        apps = apps,
                        onEditLayout = {
                            homeEdit = true
                            onScreenChange(Screen.Home)
                        },
                    )
                }
            }

            if (!prefs.showSystemStatusBar) {
                StatusBar(
                    status = status,
                    opacity = prefs.glassOpacity,
                    showDate = prefs.showDate,
                    onWifiClick = { openWifiSettings(context) },
                    onBluetoothClick = { openBluetoothSettings(context) },
                    onBatteryClick = { openBatterySettings(context) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(sides),
                )
            }
        }
    }
}
