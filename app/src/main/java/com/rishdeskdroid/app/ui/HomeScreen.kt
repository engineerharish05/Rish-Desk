package com.rishdeskdroid.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.addToDock
import com.rishdeskdroid.app.data.HOME_COLUMNS
import com.rishdeskdroid.app.data.HOME_PER_PAGE
import com.rishdeskdroid.app.data.HOME_ROWS
import com.rishdeskdroid.app.data.openOtgDestination
import com.rishdeskdroid.app.data.pruneHomeShortcuts
import com.rishdeskdroid.app.data.removeFromHome
import com.rishdeskdroid.app.data.dockApps
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.openLauncherChooser
import com.rishdeskdroid.app.data.openMobileSettings
import com.rishdeskdroid.app.data.removeFromDock
import com.rishdeskdroid.app.data.moveDockItem

/**
 * The home screen: widgets in the middle, the glass dock along the bottom.
 * Swipe up anywhere (or tap the grid tile) to open the app drawer.
 */
@Composable
fun HomeScreen(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    widgets: WidgetController,
    otgConnected: Boolean,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showGlass by remember { mutableStateOf(false) }
    var showAddPicker by remember { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -90f) onOpenDrawer() },
                    onDragCancel = { total = 0f },
                    onVerticalDrag = { _, delta -> total += delta },
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            // Widgets (only if enabled and at least one has been added)
            if (prefs.widgetsEnabled && prefs.widgetIds.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    prefs.widgetIds.forEach { id ->
                        key(id) { WidgetView(widgets, id) }
                    }
                }
            }

            // App shortcuts: 5 columns x 4 rows per page, between the widgets and the dock
            ShortcutGrid(
                prefs = prefs,
                apps = apps,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            // Keeps the grid clear of the dock, which floats over the bottom of this screen
            Spacer(Modifier.height(DockReserve))
        }

        // Dock
        Dock(
            prefs = prefs,
            apps = apps,
            onGlass = { showGlass = true },
            onAdd = { showAddPicker = true },
            onOpenDrawer = onOpenDrawer,
            onOpenSettings = onOpenSettings,
            otgConnected = otgConnected,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        )

        // Glass settings popover (the handle at the left end of the dock)
        if (showGlass) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showGlass = false },
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 84.dp)
                    .width(280.dp)
                    .glass(RoundedCornerShape(20.dp), prefs.glassOpacity, dark = true)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Glass opacity",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${(prefs.glassOpacity * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                GlassSlider(
                    value = prefs.glassOpacity,
                    onValueChange = { prefs.updateGlassOpacity(it) },
                )
            }
        }
    }

    if (showAddPicker) {
        val pinned = dockApps(prefs, apps).map { it.packageName }
        AppPickerDialog(
            title = "Add to dock",
            apps = apps.filter { it.packageName !in pinned },
            onPick = { app ->
                showAddPicker = false
                if (!addToDock(prefs, apps, app)) {
                    Toast.makeText(context, "The dock is full", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showAddPicker = false },
        )
    }
}

@Composable
fun GlassSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = 0f..1f,
        modifier = modifier.semantics { contentDescription = "Glass opacity" },
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color(0xFF7AA7FF),
            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
        ),
    )
}

@Composable
private fun Dock(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    onGlass: () -> Unit,
    onAdd: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    otgConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dock = dockApps(prefs, apps)
    val tile = 48.dp

    Row(
        modifier
            .glass(RoundedCornerShape(26.dp), prefs.glassOpacity)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Glass settings handle
        val handleSource = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(width = 24.dp, height = tile)
                .pressScale(handleSource, 1.2f)
                .clickable(interactionSource = handleSource, indication = null) { onGlass() }
                .semantics { contentDescription = "Glass settings" },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 26.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
            )
        }

        // Mobile settings
        IconTile(tile, Tiles.gray, "Mobile settings", onClick = { openMobileSettings(context) }) {
            SvgIcon(Ico.gear, 26.dp)
        }

        // Pinned apps: Chrome, YouTube, Google, GPT by default
        dock.forEachIndexed { index, app ->
            key(app.key) {
                DockApp(
                    app = app,
                    tile = tile,
                    canMoveLeft = index > 0,
                    canMoveRight = index < dock.lastIndex,
                    onMoveLeft = { moveDockItem(prefs, apps, app.packageName, -1) },
                    onMoveRight = { moveDockItem(prefs, apps, app.packageName, 1) },
                    onRemove = { removeFromDock(prefs, apps, app) },
                )
            }
        }

        // OTG shortcut: opens Android's storage / USB drive screen (with fallbacks), not our Settings
        if (prefs.otgEnabled && otgConnected) {
            IconTile(tile, Tiles.green, "USB / OTG storage", onClick = { openOtgDestination(context) }) {
                SvgIcon(Ico.usb, 26.dp)
            }
        }

        // + add an app
        IconTile(
            size = tile,
            brush = Tiles.glass,
            label = "Add an app",
            onClick = onAdd,
            decoration = Modifier.dashedBorder(Color.White.copy(alpha = 0.75f), tile * 0.24f),
        ) {
            SvgIcon(Ico.plus, 24.dp, strokeWidth = 2.2f)
        }

        // App drawer
        IconTile(tile, Tiles.blue, "App drawer", onClick = onOpenDrawer) {
            SvgIcon(Ico.grid, 26.dp)
        }

        Box(
            Modifier
                .size(width = 1.dp, height = 38.dp)
                .background(Color.White.copy(alpha = 0.45f)),
        )

        // App settings
        IconTile(tile, Tiles.graphite, "App settings", onClick = onOpenSettings) {
            SvgIcon(Ico.sliders, 26.dp)
        }

        // Exit / switch launcher
        IconTile(tile, Tiles.red, "Exit launcher", onClick = { openLauncherChooser(context) }) {
            SvgIcon(Ico.exit, 26.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockApp(
    app: AppInfo,
    tile: androidx.compose.ui.unit.Dp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }

    Box {
        Box(
            Modifier
                .size(tile)
                .pressScale(source)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { launchApp(context, app) },
                    onLongClick = { menu = true },
                )
                .semantics { contentDescription = app.label },
        ) {
            AppIconImage(app, tile)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (canMoveLeft) {
                DropdownMenuItem(
                    text = { Text("Move left") },
                    onClick = { menu = false; onMoveLeft() },
                )
            }
            if (canMoveRight) {
                DropdownMenuItem(
                    text = { Text("Move right") },
                    onClick = { menu = false; onMoveRight() },
                )
            }
            DropdownMenuItem(
                text = { Text("Remove from dock") },
                onClick = {
                    menu = false
                    onRemove()
                },
            )
        }
    }
}


/** Space reserved at the bottom of the home screen for the floating dock (tile + padding). */
private val DockReserve = 88.dp

/**
 * Home-screen shortcuts: a 5 x 4 grid per page. A new page appears automatically once a page is full.
 * Shortcuts are stored by package name and slot number, and looked up in the installed-app list.
 */
@Composable
private fun ShortcutGrid(prefs: LauncherPrefs, apps: List<AppInfo>, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Drop shortcuts for apps that are really gone. The wait avoids pruning during an app update,
    // when the package briefly disappears.
    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            delay(3000)
            pruneHomeShortcuts(prefs, context.packageManager)
        }
    }

    val installed = remember(apps) { apps.distinctBy { it.packageName }.associateBy { it.packageName } }
    val shortcuts = prefs.homeShortcuts
    val pageCount = (shortcuts.maxOfOrNull { it.slot } ?: 0) / HOME_PER_PAGE + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            ShortcutPage(prefs, page, installed)
        }
        if (pageCount > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .semantics { contentDescription = "Home page ${pagerState.currentPage + 1} of $pageCount" },
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                for (i in 0 until pageCount) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (i == pagerState.currentPage) 1f else 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutPage(prefs: LauncherPrefs, page: Int, installed: Map<String, AppInfo>) {
    val bySlot = prefs.homeShortcuts.associateBy { it.slot }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        val iconSize = (maxHeight / HOME_ROWS - 24.dp).coerceIn(24.dp, 52.dp)
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until HOME_ROWS) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until HOME_COLUMNS) {
                        val slot = page * HOME_PER_PAGE + row * HOME_COLUMNS + col
                        val app = bySlot[slot]?.let { installed[it.packageName] }
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app != null) key(app.packageName) { ShortcutCell(prefs, app, iconSize) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutCell(prefs: LauncherPrefs, app: AppInfo, iconSize: Dp) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }

    Box {
        Column(
            Modifier
                .pressScale(source, 1.07f)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { launchApp(context, app) },
                    onLongClick = { menu = true },
                )
                .semantics { contentDescription = app.label }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AppIconImage(app, iconSize)
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = LabelStyle,
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            // Only removes the shortcut; the app stays installed and in the drawer
            DropdownMenuItem(
                text = { Text("Remove from Home") },
                onClick = {
                    menu = false
                    removeFromHome(prefs, app.packageName)
                },
            )
        }
    }
}
