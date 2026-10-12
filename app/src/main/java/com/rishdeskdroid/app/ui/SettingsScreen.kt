package com.rishdeskdroid.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.rishdeskdroid.app.data.ACCENT_COLORS
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.DockAlign
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.RecentStore
import com.rishdeskdroid.app.data.SortMode
import com.rishdeskdroid.app.data.WallpaperFit
import com.rishdeskdroid.app.data.WallpaperStore
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.canWriteSettings
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.openBluetoothSettings
import com.rishdeskdroid.app.data.openDeveloperSettings
import com.rishdeskdroid.app.data.openDisplaySettings
import com.rishdeskdroid.app.data.openFiles
import com.rishdeskdroid.app.data.openLauncherChooser
import com.rishdeskdroid.app.data.openNotificationSettings
import com.rishdeskdroid.app.data.openStorageSettings
import com.rishdeskdroid.app.data.openWifiSettings
import com.rishdeskdroid.app.data.openWriteSettings
import com.rishdeskdroid.app.data.storageSummary
import com.rishdeskdroid.app.data.supportsFreeform
import com.rishdeskdroid.app.data.terminalApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ResetKind(val title: String, val message: String, val confirm: String) {
    APPEARANCE(
        "Reset appearance?",
        "Glass opacity, accent colour, animations, date and wallpaper adjustments go back to their defaults. Your wallpaper photo is kept.",
        "Reset appearance",
    ),
    DESKTOP(
        "Reset desktop layout?",
        "Home shortcuts, folders, widget sizes, grid size and icon settings go back to their defaults. Apps are not uninstalled.",
        "Reset desktop",
    ),
    DOCK(
        "Reset dock?",
        "The dock goes back to its default apps. Apps are not uninstalled.",
        "Reset dock",
    ),
    ALL(
        "Reset all launcher settings?",
        "Every launcher setting, shortcut, folder, favorite and hidden app is cleared. Apps are not uninstalled and your wallpaper photo and widgets are kept.",
        "Reset everything",
    ),
}

/**
 * Settings: appearance, desktop, drawer, dock, widgets, wallpaper, keyboard, devices, system
 * integration, privacy and resets. Leave with the back button.
 */
@Composable
fun SettingsScreen(
    prefs: LauncherPrefs,
    widgets: WidgetController,
    apps: List<AppInfo>,
    onEditLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showWallpaper by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf<ResetKind?>(null) }

    // Re-check the "Modify system settings" permission when coming back from Android's screen
    var writeGranted by remember { mutableStateOf(canWriteSettings(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) writeGranted = canWriteSettings(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Wallpaper: the photo picked but not applied yet
    var pending by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingRaw by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val current by produceState<ImageBitmap?>(initialValue = null, key1 = prefs.wallpaperVersion) {
        value = withContext(Dispatchers.IO) { WallpaperStore.load(context) }
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && showWallpaper) {
            scope.launch {
                val bitmap = try {
                    withContext(Dispatchers.IO) { WallpaperStore.decode(context, uri) }
                } catch (e: Exception) {
                    null
                } catch (e: OutOfMemoryError) {
                    null
                }
                if (bitmap != null) {
                    pendingRaw = bitmap
                    pending = bitmap.asImageBitmap()
                } else {
                    android.widget.Toast.makeText(context, "That image can't be used as a wallpaper", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            // Swallow taps so nothing underneath reacts
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 28.dp),
    ) {
        Text(
            "Settings",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            style = LabelStyle,
            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ---- Appearance -------------------------------------------------------------------
            Section("Appearance", prefs) {
                SettingRow(Ico.drop, Tiles.sky, "Glass Opacity") {
                    GlassSlider(
                        value = prefs.glassOpacity,
                        onValueChange = { prefs.updateGlassOpacity(it) },
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                    Text(
                        "${(prefs.glassOpacity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(44.dp),
                    )
                }
                RowDivider()
                SettingBlock(Ico.image, Tiles.pink, "Accent colour", "Used for buttons, sliders and the keyboard focus ring") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ACCENT_COLORS.forEach { argb ->
                            val selected = argb == prefs.accent
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(if (selected) 3.dp else 1.dp, Color.White.copy(alpha = if (selected) 1f else 0.4f), CircleShape)
                                    .clickable(role = Role.RadioButton) { prefs.updateAccent(argb) }
                                    .semantics { contentDescription = if (selected) "Accent colour, selected" else "Accent colour" },
                            )
                        }
                    }
                }
                RowDivider()
                SettingRow(Ico.sliders, Tiles.graphite, "Animations", "Turn off to reduce motion") {
                    GlassSwitch(prefs.animations, { prefs.updateAnimations(it) }, "Animations")
                }
                RowDivider()
                SettingRow(Ico.clock, Tiles.blue, "Show date", "Adds the date next to the time in the status bar") {
                    GlassSwitch(prefs.showDate, { prefs.updateShowDate(it) }, "Show date")
                }
                RowDivider()
                SettingRow(
                    Ico.gear, Tiles.gray, "Android status bar",
                    "Show Android's own bar instead of the launcher's. Either way, swipe down from the top edge for notifications and quick settings.",
                ) {
                    GlassSwitch(prefs.showSystemStatusBar, { prefs.updateShowSystemStatusBar(it) }, "Android status bar")
                }
                RowDivider()
                SettingRow(Ico.gear, Tiles.gray, "Notifications", "Opens Android's notification settings") {
                    GlassButton("Open") { openNotificationSettings(context) }
                }
            }

            // ---- Desktop ----------------------------------------------------------------------
            Section("Desktop", prefs) {
                SettingRow(Ico.grid, Tiles.blue, "Layout editing", "Drag icons to move them. Drop one on another to make a folder.") {
                    GlassButton("Edit layout", primary = true, onClick = onEditLayout)
                }
                RowDivider()
                SettingRow(Ico.grid, Tiles.graphite, "Columns") {
                    Stepper(prefs.desktopColumns, 3, 7, "desktop columns") {
                        prefs.updateDesktopGrid(it, prefs.desktopRows)
                    }
                }
                RowDivider()
                SettingRow(Ico.grid, Tiles.graphite, "Rows") {
                    Stepper(prefs.desktopRows, 2, 6, "desktop rows") {
                        prefs.updateDesktopGrid(prefs.desktopColumns, it)
                    }
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Icon size") {
                    GlassSlider(
                        value = prefs.desktopIconScale,
                        onValueChange = { prefs.updateDesktopIconScale(it) },
                        description = "Desktop icon size",
                        valueRange = 0.7f..1.3f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Icon spacing") {
                    GlassSlider(
                        value = prefs.desktopSpacing.toFloat(),
                        onValueChange = { prefs.updateDesktopSpacing(it.toInt()) },
                        description = "Desktop icon spacing",
                        valueRange = 0f..24f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.keyboard, Tiles.graphite, "Show labels") {
                    GlassSwitch(prefs.showDesktopLabels, { prefs.updateShowDesktopLabels(it) }, "Desktop labels")
                }
            }

            // ---- App drawer -------------------------------------------------------------------
            Section("App drawer", prefs) {
                SettingBlock(Ico.grid, Tiles.blue, "Sort order", "Recently used only counts apps opened from this launcher, so no extra permission is needed") {
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        Segmented(SortMode.entries, prefs.sortMode, { it.label }) { prefs.updateSortMode(it) }
                    }
                }
                RowDivider()
                SettingRow(Ico.grid, Tiles.graphite, "Columns") {
                    Stepper(prefs.drawerColumns, 3, 8, "drawer columns") {
                        prefs.updateDrawerGrid(it, prefs.drawerRows)
                    }
                }
                RowDivider()
                SettingRow(Ico.grid, Tiles.graphite, "Rows") {
                    Stepper(prefs.drawerRows, 2, 5, "drawer rows") {
                        prefs.updateDrawerGrid(prefs.drawerColumns, it)
                    }
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Icon size") {
                    GlassSlider(
                        value = prefs.drawerIconScale,
                        onValueChange = { prefs.updateDrawerIconScale(it) },
                        description = "Drawer icon size",
                        valueRange = 0.7f..1.3f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.keyboard, Tiles.graphite, "Show labels") {
                    GlassSwitch(prefs.showDrawerLabels, { prefs.updateShowDrawerLabels(it) }, "Drawer labels")
                }
                RowDivider()
                SettingRow(Ico.close, Tiles.red, "Hidden apps", "${prefs.hiddenApps.size} hidden. They stay installed.") {
                    GlassButton("Manage") { showHidden = true }
                }
                RowDivider()
                SettingRow(Ico.clock, Tiles.blue, "Recent apps", "Forget the list of apps opened from here") {
                    GlassButton("Clear") {
                        RecentStore.clear(context)
                        android.widget.Toast.makeText(context, "Recent apps cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // ---- Dock and taskbar -------------------------------------------------------------
            Section("Dock and taskbar", prefs) {
                SettingBlock(Ico.grid, Tiles.blue, "Dock position") {
                    Segmented(DockAlign.entries, prefs.dockAlign, { it.label }) { prefs.updateDockAlign(it) }
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Dock icon size") {
                    GlassSlider(
                        value = prefs.dockScale,
                        onValueChange = { prefs.updateDockScale(it) },
                        description = "Dock icon size",
                        valueRange = 0.8f..1.3f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.clock, Tiles.sky, "Recent apps button", "Shows a recent-apps panel in the dock") {
                    GlassSwitch(prefs.showRecentsButton, { prefs.updateShowRecentsButton(it) }, "Recent apps button")
                }
                RowDivider()
                SettingRow(Ico.exit, Tiles.red, "Dock apps", "Long-press a dock app to move or remove it. Restore the default apps here.") {
                    GlassButton("Reset dock") { reset = ResetKind.DOCK }
                }
            }

            // ---- Widgets ----------------------------------------------------------------------
            Section("Widgets", prefs) {
                SettingRow(Ico.widgets, Tiles.blue, "Widgets", "Use Edit layout on the home screen to move or resize them") {
                    Row(
                        Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        prefs.widgetIds.forEach { id ->
                            val label = remember(id) {
                                widgets.infoOf(id)?.loadLabel(context.packageManager)?.toString() ?: "Widget"
                            }
                            WidgetChip(label) { widgets.remove(id) }
                        }
                    }
                    SquareButton("Select widget", onClick = { showWidgetPicker = true }) {
                        SvgIcon(Ico.plus, 22.dp, strokeWidth = 2.2f)
                    }
                    GlassSwitch(prefs.widgetsEnabled, { prefs.updateWidgetsEnabled(it) }, "Widgets")
                }
            }

            // ---- Wallpaper --------------------------------------------------------------------
            Section("Wallpaper", prefs) {
                SettingRow(Ico.image, Tiles.pink, "Wallpaper change", "Tap the preview to choose a photo") {
                    val thumbShape = RoundedCornerShape(8.dp)
                    val shown = current
                    Box(
                        Modifier
                            .size(width = 84.dp, height = 48.dp)
                            .clip(thumbShape)
                            .border(1.dp, Color.White.copy(alpha = 0.5f), thumbShape)
                            .clickable(onClickLabel = "Open wallpaper controls", role = Role.Button) { showWallpaper = true }
                            .semantics { contentDescription = "Wallpaper preview. Tap to change wallpaper" },
                    ) {
                        if (shown != null) {
                            WallpaperImage(shown, prefs.wallpaperFit, prefs.wallpaperX, prefs.wallpaperY, Modifier.fillMaxSize())
                        } else {
                            DefaultWallpaper(Modifier.fillMaxSize())
                        }
                    }
                    GlassButton("Change") { showWallpaper = true }
                }
                RowDivider()
                SettingBlock(Ico.image, Tiles.pink, "Fit", "Fill crops to cover the screen, Fit shows the whole photo, Stretch fills without keeping proportions") {
                    Segmented(WallpaperFit.entries, prefs.wallpaperFit, { it.label }) { prefs.updateWallpaperFit(it) }
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Position left / right", "Which part of the photo stays in view when it is cropped") {
                    GlassSlider(
                        value = prefs.wallpaperX,
                        onValueChange = { prefs.updateWallpaperPosition(it, prefs.wallpaperY) },
                        description = "Wallpaper horizontal position",
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.image, Tiles.sky, "Position up / down") {
                    GlassSlider(
                        value = prefs.wallpaperY,
                        onValueChange = { prefs.updateWallpaperPosition(prefs.wallpaperX, it) },
                        description = "Wallpaper vertical position",
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.drop, Tiles.graphite, "Dim", "Darkens the wallpaper so icons and text stand out") {
                    GlassSlider(
                        value = prefs.wallpaperDim,
                        onValueChange = { prefs.updateWallpaperDim(it) },
                        description = "Wallpaper dim",
                        valueRange = 0f..0.8f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
                RowDivider()
                SettingRow(Ico.drop, Tiles.graphite, "Blur", "Works on Android 12 and newer") {
                    GlassSlider(
                        value = prefs.wallpaperBlur.toFloat(),
                        onValueChange = { prefs.updateWallpaperBlur(it.toInt()) },
                        description = "Wallpaper blur",
                        valueRange = 0f..25f,
                        modifier = Modifier.widthIn(max = 320.dp).weight(1f, fill = false),
                    )
                }
            }

            // ---- Keyboard and mouse -----------------------------------------------------------
            Section("Keyboard and mouse", prefs) {
                SettingRow(
                    Ico.keyboard, Tiles.graphite, "Keyboard shortcuts",
                    "Ctrl, Alt or Win + D drawer · F or / search · , settings · H home · 1-9 dock apps · Esc back. Arrow keys and Tab move between icons, Enter opens.",
                ) {
                    GlassSwitch(prefs.keyboardEnabled, { prefs.updateKeyboardEnabled(it) }, "Keyboard shortcuts")
                }
            }

            // ---- External devices and files ---------------------------------------------------
            Section("USB, OTG and files", prefs) {
                SettingRow(
                    Ico.usb, Tiles.green, "OTG Shortcut",
                    "Adds a dock shortcut to the Android USB / storage screen while a USB device is plugged in",
                ) {
                    GlassSwitch(prefs.otgEnabled, { prefs.updateOtgEnabled(it) }, "OTG Shortcut")
                }
                RowDivider()
                val storage = remember { storageSummary(context) }
                SettingRow(Ico.usb, Tiles.blue, "Files and storage", storage ?: "Storage details are not available") {
                    GlassButton("Open files") { openFiles(context) }
                    GlassButton("Storage") { openStorageSettings(context) }
                }
            }

            // ---- System and apps --------------------------------------------------------------
            Section("System and apps", prefs) {
                SettingRow(
                    Ico.exit, Tiles.blue, "Open apps in landscape",
                    if (writeGranted) {
                        "Locks rotation to landscape while you use an app, then puts your own setting back. Apps that insist on portrait can still stay portrait."
                    } else {
                        "Needs the \"Modify system settings\" permission. Only the rotation setting is changed, and it is put back when you return."
                    },
                ) {
                    if (!writeGranted && prefs.landscapeApps) {
                        GlassButton("Allow") { openWriteSettings(context) }
                    }
                    GlassSwitch(prefs.landscapeApps, { prefs.updateLandscapeApps(it) }, "Open apps in landscape")
                }
                RowDivider()
                val freeform = remember { supportsFreeform(context) }
                SettingRow(
                    Ico.widgets, Tiles.graphite, "Floating windows (experimental)",
                    if (freeform) {
                        "Opens apps in a floating window. Only apps that allow resizing will behave."
                    } else {
                        "Not available: this device does not offer floating windows to launchers."
                    },
                ) {
                    GlassSwitch(
                        checked = prefs.windowedApps && freeform,
                        onCheckedChange = { if (freeform) prefs.updateWindowedApps(it) },
                        label = "Floating windows",
                    )
                }
                RowDivider()
                SettingBlock(Ico.gear, Tiles.gray, "System shortcuts") {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("Display") { openDisplaySettings(context) }
                        GlassButton("Wi-Fi") { openWifiSettings(context) }
                        GlassButton("Bluetooth") { openBluetoothSettings(context) }
                        GlassButton("Developer options") { openDeveloperSettings(context) }
                        GlassButton("Terminal") {
                            val terminal = terminalApp(apps)
                            if (terminal != null) {
                                launchApp(context, terminal)
                            } else {
                                android.widget.Toast.makeText(context, "No terminal app is installed", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                RowDivider()
                SettingRow(
                    Ico.exit, Tiles.red, "Default launcher",
                    "To make Rish Deskdroid your home app, press Home and choose it with \"Always\", or pick it in Android's default home app screen.",
                ) {
                    GlassButton("Open") { openLauncherChooser(context) }
                }
            }

            // ---- Privacy and permissions ------------------------------------------------------
            Section("Privacy and permissions", prefs) {
                InfoText(
                    "Everything stays on this device. Rish Deskdroid has no internet permission, collects nothing and sends nothing.\n\n" +
                        "Permissions it asks for, and why:\n" +
                        "• Modify system settings: only to lock rotation to landscape for apps you open (optional).\n" +
                        "• Network and Bluetooth state: to show Wi-Fi, mobile signal and Bluetooth icons in the status bar.\n" +
                        "• Request delete packages: lets the Uninstall action show Android's confirmation.\n\n" +
                        "Android does not let launchers float arbitrary apps, list other apps' running windows, or read which apps you used without a special usage-access permission, so those are not offered.",
                )
            }

            // ---- Reset ------------------------------------------------------------------------
            Section("Reset", prefs) {
                SettingRow(Ico.sliders, Tiles.graphite, "Appearance") {
                    GlassButton("Reset") { reset = ResetKind.APPEARANCE }
                }
                RowDivider()
                SettingRow(Ico.grid, Tiles.graphite, "Desktop layout") {
                    GlassButton("Reset") { reset = ResetKind.DESKTOP }
                }
                RowDivider()
                SettingRow(Ico.exit, Tiles.red, "All launcher settings") {
                    GlassButton("Reset all") { reset = ResetKind.ALL }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showWallpaper) {
        WallpaperDialog(
            preview = pending ?: current,
            fit = prefs.wallpaperFit,
            x = prefs.wallpaperX,
            y = prefs.wallpaperY,
            hasCustomWallpaper = current != null,
            hasPending = pendingRaw != null,
            onSelectPhoto = { pickPhoto.launch("image/*") },
            onUseDefault = {
                pending = null
                pendingRaw = null
                WallpaperStore.clear(context)
                prefs.bumpWallpaper()
                showWallpaper = false
            },
            onApply = {
                val raw = pendingRaw
                if (raw != null) {
                    scope.launch {
                        val saved = try {
                            withContext(Dispatchers.IO) { WallpaperStore.save(context, raw) }
                            true
                        } catch (e: Exception) {
                            false
                        }
                        if (saved) {
                            pending = null
                            pendingRaw = null
                            prefs.bumpWallpaper()
                            showWallpaper = false
                        } else {
                            android.widget.Toast.makeText(context, "Couldn't save the wallpaper", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onCancel = {
                // Closing without Apply keeps the current wallpaper untouched
                pending = null
                pendingRaw = null
                showWallpaper = false
            },
        )
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            providers = remember { widgets.providers() },
            onPick = { info ->
                showWidgetPicker = false
                widgets.beginAdd(info)
            },
            onDismiss = { showWidgetPicker = false },
        )
    }

    if (showHidden) HiddenAppsDialog(prefs, apps) { showHidden = false }

    reset?.let { kind ->
        ConfirmDialog(
            title = kind.title,
            message = kind.message,
            confirmLabel = kind.confirm,
            onConfirm = {
                when (kind) {
                    ResetKind.APPEARANCE -> prefs.resetAppearance()
                    ResetKind.DESKTOP -> prefs.resetDesktopLayout()
                    ResetKind.DOCK -> prefs.resetDock()
                    ResetKind.ALL -> prefs.resetAll()
                }
                reset = null
            },
            onDismiss = { reset = null },
        )
    }
}

@Composable
private fun Section(title: String, prefs: LauncherPrefs, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            style = LabelStyle,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .glass(RoundedCornerShape(22.dp), prefs.glassOpacity),
            content = content,
        )
    }
}

@Composable
private fun IconBadge(icon: IconDef, brush: Brush) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(icon, 22.dp)
    }
}

@Composable
private fun TitleBlock(title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            style = LabelStyle,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/** A setting with its control on the right. */
@Composable
private fun SettingRow(
    icon: IconDef,
    brush: Brush,
    title: String,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, brush)
        TitleBlock(
            title,
            subtitle,
            Modifier
                .padding(start = 14.dp, end = 12.dp)
                .widthIn(min = 150.dp, max = 280.dp),
        )
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing,
        )
    }
}

/** A setting whose control is wide, so it sits underneath the title. */
@Composable
private fun SettingBlock(
    icon: IconDef,
    brush: Brush,
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, brush)
            TitleBlock(title, subtitle, Modifier.padding(start = 14.dp))
        }
        content()
    }
}

@Composable
private fun InfoText(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = Modifier.padding(18.dp),
    )
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.14f)),
    )
}

@Composable
private fun WidgetChip(label: String, onRemove: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .heightIn(min = 32.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClickLabel = "Remove $label", onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, maxLines = 1)
        SvgIcon(Ico.close, 14.dp, strokeWidth = 2.4f)
    }
}

@Composable
private fun SquareButton(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.2f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Wallpaper controls: pick a photo, preview it, then Apply, or go back to the default / cancel. */
@Composable
private fun WallpaperDialog(
    preview: ImageBitmap?,
    fit: WallpaperFit,
    x: Float,
    y: Float,
    hasCustomWallpaper: Boolean,
    hasPending: Boolean,
    onSelectPhoto: () -> Unit,
    onUseDefault: () -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val shape = RoundedCornerShape(24.dp)
        Row(
            Modifier
                .fillMaxWidth(0.8f)
                .background(Color(0xF2171A3F), shape)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val previewShape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(16f / 9f)
                    .clip(previewShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), previewShape)
                    .semantics {
                        contentDescription =
                            if (hasPending) "Selected wallpaper preview, not applied yet" else "Current wallpaper preview"
                    },
            ) {
                if (preview != null) {
                    WallpaperImage(preview, fit, x, y, Modifier.fillMaxSize())
                } else {
                    DefaultWallpaper(Modifier.fillMaxSize())
                }
            }
            Column(
                Modifier.widthIn(min = 150.dp, max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PanelTitle("Wallpaper")
                GlassButton("Select photo", fullWidth = true, onClick = onSelectPhoto)
                GlassButton("Use default", enabled = hasCustomWallpaper || hasPending, fullWidth = true, onClick = onUseDefault)
                GlassButton("Apply", primary = true, enabled = hasPending, fullWidth = true, onClick = onApply)
                GlassButton("Cancel", fullWidth = true, onClick = onCancel)
            }
        }
    }
}
