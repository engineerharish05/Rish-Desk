package com.rishdeskdroid.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.HomeApp
import com.rishdeskdroid.app.data.HomeFolder
import com.rishdeskdroid.app.data.HomeItem
import com.rishdeskdroid.app.data.HomeLayout
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.WIDGET_UNIT_DP
import com.rishdeskdroid.app.data.WidgetController
import com.rishdeskdroid.app.data.DockAlign
import com.rishdeskdroid.app.data.addToDock
import com.rishdeskdroid.app.data.dockApps
import com.rishdeskdroid.app.data.homePerPage
import com.rishdeskdroid.app.data.labelFor
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.moveInDock
import com.rishdeskdroid.app.data.openLauncherChooser
import com.rishdeskdroid.app.data.openMobileSettings
import com.rishdeskdroid.app.data.openOtgDestination
import com.rishdeskdroid.app.data.pruneHome
import com.rishdeskdroid.app.data.recentApps
import com.rishdeskdroid.app.data.removeFromDock
import com.rishdeskdroid.app.data.removeFromHome
import com.rishdeskdroid.app.data.widgetSpanLimits
import kotlinx.coroutines.delay
import kotlin.math.floor

/** Which small popover is open above the dock. */
private enum class Popup { None, Glass, Recents }

/**
 * The home screen: widgets along the top, the shortcut grid in the middle, the glass dock along
 * the bottom. Swipe up (or tap the grid tile) to open the app drawer.
 */
@Composable
fun HomeScreen(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    widgets: WidgetController,
    otgConnected: Boolean,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var popup by remember { mutableStateOf(Popup.None) }
    var showAddPicker by remember { mutableStateOf(false) }
    var openFolderId by remember { mutableStateOf<String?>(null) }
    var renameApp by remember { mutableStateOf<AppInfo?>(null) }

    val dockReserve = (48 * prefs.dockScale + 40).dp
    val hasWorkspaceContent = prefs.homeItems.isNotEmpty() ||
        (prefs.widgetsEnabled && prefs.widgetIds.isNotEmpty())

    // Handlers registered later win: the popover closes first, then edit mode
    BackHandler(enabled = editMode) { onEditModeChange(false) }
    BackHandler(enabled = popup != Popup.None) { popup = Popup.None }
    // Back on the home screen itself is consumed by LauncherRoot, so it never closes the launcher.

    Box(
        modifier
            .fillMaxSize()
            .pointerInput(editMode) {
                if (!editMode) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -90f) onOpenDrawer() },
                        onDragCancel = { total = 0f },
                        onVerticalDrag = { _, delta -> total += delta },
                    )
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            if (prefs.widgetsEnabled && prefs.widgetIds.isNotEmpty()) {
                WidgetStrip(prefs, widgets, editMode)
            }
            ShortcutGrid(
                prefs = prefs,
                apps = apps,
                editMode = editMode,
                onEnterEdit = { onEditModeChange(true) },
                onOpenFolder = { openFolderId = it },
                onRename = { renameApp = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            // Keeps the grid clear of the dock, which floats over the bottom of this screen
            Spacer(Modifier.height(dockReserve))
        }

        if (!hasWorkspaceContent && !editMode) {
            Text(
                "Your desktop is empty. Open the app drawer, long-press an app and choose Add to Home.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                style = LabelStyle,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 48.dp),
            )
        }

        val dockAlignment = when (prefs.dockAlign) {
            DockAlign.START -> Alignment.BottomStart
            DockAlign.CENTER -> Alignment.BottomCenter
            DockAlign.END -> Alignment.BottomEnd
        }
        val dockModifier = Modifier
            .align(dockAlignment)
            .padding(horizontal = 8.dp, vertical = 12.dp)

        if (editMode) {
            EditBar(prefs, onDone = { onEditModeChange(false) }, modifier = dockModifier)
        } else {
            Dock(
                prefs = prefs,
                apps = apps,
                onGlass = { popup = Popup.Glass },
                onRecents = { popup = Popup.Recents },
                onAdd = { showAddPicker = true },
                onOpenDrawer = onOpenDrawer,
                onOpenSettings = onOpenSettings,
                otgConnected = otgConnected,
                modifier = dockModifier,
            )
        }

        if (popup != Popup.None) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClickLabel = "Close",
                    ) { popup = Popup.None },
            )
            val popupAlignment = when (prefs.dockAlign) {
                DockAlign.START -> Alignment.BottomStart
                DockAlign.CENTER -> Alignment.BottomCenter
                DockAlign.END -> Alignment.BottomEnd
            }
            Column(
                Modifier
                    .align(popupAlignment)
                    .padding(horizontal = 12.dp)
                    .padding(bottom = dockReserve)
                    .glass(RoundedCornerShape(20.dp), prefs.glassOpacity, dark = true)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                if (popup == Popup.Glass) {
                    GlassPopover(prefs)
                } else {
                    RecentsPopover(prefs, apps, onPicked = { popup = Popup.None })
                }
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

    openFolderId?.let { id ->
        FolderDialog(prefs = prefs, folderId = id, apps = apps, onDismiss = { openFolderId = null })
    }

    renameApp?.let { app ->
        RenameDialog(
            title = "Rename shortcut",
            initial = prefs.customLabels[app.packageName].orEmpty(),
            hint = app.label,
            onSave = {
                prefs.updateCustomLabel(app.packageName, it)
                renameApp = null
            },
            onDismiss = { renameApp = null },
        )
    }
}

@Composable
private fun GlassPopover(prefs: LauncherPrefs) {
    Column(Modifier.width(280.dp)) {
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

/** Recently opened apps (opened from this launcher). The last one opened is marked. */
@Composable
private fun RecentsPopover(prefs: LauncherPrefs, apps: List<AppInfo>, onPicked: () -> Unit) {
    val context = LocalContext.current
    val recents = recentApps(prefs, apps).take(8)
    Text(
        "Recent apps",
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    if (recents.isEmpty()) {
        Text(
            "Apps you open from the launcher will appear here.",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 13.sp,
            modifier = Modifier.width(280.dp),
        )
    } else {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            recents.forEachIndexed { index, app ->
                key(app.packageName) {
                    Column(
                        Modifier
                            .clickable {
                                onPicked()
                                launchApp(context, app)
                            }
                            .semantics { contentDescription = app.label }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AppIconImage(app, 44.dp)
                        Text(
                            app.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(60.dp),
                            textAlign = TextAlign.Center,
                        )
                        if (index == 0) {
                            Text("Last opened", color = LocalAccent.current, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, description: String = "Glass opacity", valueRange: ClosedFloatingPointRange<Float> = 0f..1f) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier.semantics { contentDescription = description },
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = LocalAccent.current,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
        ),
    )
}

// ---- Widgets ------------------------------------------------------------------------------------

@Composable
private fun WidgetStrip(prefs: LauncherPrefs, widgets: WidgetController, editMode: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        prefs.widgetIds.forEach { id ->
            key(id) { WidgetSlot(prefs, widgets, id, editMode) }
        }
    }
}

@Composable
private fun WidgetSlot(prefs: LauncherPrefs, widgets: WidgetController, id: Int, editMode: Boolean) {
    val context = LocalContext.current
    val info = widgets.infoOf(id) ?: return
    val density = LocalDensity.current.density
    val limits = remember(info, density) { widgetSpanLimits(info, density) }
    val size = limits.clamp(prefs.widgetSizes[id])
    val width = (size.w * WIDGET_UNIT_DP).dp
    val height = (size.h * WIDGET_UNIT_DP).dp
    val index = prefs.widgetIds.indexOf(id)
    var showResize by remember { mutableStateOf(false) }

    Box(Modifier.size(width, height)) {
        WidgetView(widgets, id, info, width, height)
        if (editMode) {
            // Blocks touches to the widget itself and shows the layout controls
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .pointerInput(Unit) { detectTapGestures { } },
            )
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MiniButton("‹", "Move widget left", index > 0) { moveWidget(prefs, index, -1) }
                MiniButton("›", "Move widget right", index < prefs.widgetIds.size - 1) { moveWidget(prefs, index, 1) }
                MiniButton("⤢", "Resize widget", true) { showResize = true }
                MiniButton("✕", "Remove widget", true) { widgets.remove(id) }
            }
        }
    }

    if (showResize) {
        val label = remember(info) { info.loadLabel(context.packageManager).toString() }
        ResizeWidgetDialog(
            label = label,
            limits = limits,
            size = size,
            onChange = { prefs.updateWidgetSizes(prefs.widgetSizes + (id to limits.clamp(it))) },
            onDismiss = { showResize = false },
        )
    }
}

private fun moveWidget(prefs: LauncherPrefs, index: Int, delta: Int) {
    val ids = prefs.widgetIds.toMutableList()
    val to = index + delta
    if (index < 0 || to !in ids.indices) return
    java.util.Collections.swap(ids, index, to)
    prefs.updateWidgetIds(ids)
}

@Composable
private fun MiniButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFF0E1028).copy(alpha = if (enabled) 0.85f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Color.White.copy(alpha = if (enabled) 1f else 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// ---- Shortcut grid ------------------------------------------------------------------------------

/**
 * Home-screen shortcuts and folders in a grid whose size comes from Settings (5 x 4 by default).
 * A new page appears when a page is full. In edit mode icons can be dragged: onto an empty cell to
 * move, onto another app to make a folder, onto a folder to add to it.
 */
@Composable
private fun ShortcutGrid(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onRename: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val perPage = homePerPage(prefs)

    // Drop shortcuts for apps that are really gone. The wait avoids pruning during an app update,
    // when the package briefly disappears.
    LaunchedEffect(apps) {
        if (apps.isNotEmpty()) {
            delay(3000)
            pruneHome(prefs, context.packageManager)
        }
    }

    val installed = remember(apps) { apps.distinctBy { it.packageName }.associateBy { it.packageName } }
    val items = prefs.homeItems
    val pageCount = (items.maxOfOrNull { it.slot } ?: 0) / perPage + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            ShortcutPage(prefs, items, page, installed, editMode, onEnterEdit, onOpenFolder, onRename)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutPage(
    prefs: LauncherPrefs,
    items: List<HomeItem>,
    page: Int,
    installed: Map<String, AppInfo>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onRename: (AppInfo) -> Unit,
) {
    val cols = prefs.desktopColumns
    val rows = prefs.desktopRows
    val perPage = cols * rows
    val bySlot = remember(items) { items.associateBy { it.slot } }
    var dragSlot by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 2.dp)) {
        val cellWidthPx = constraints.maxWidth.toFloat() / cols
        val cellHeightPx = constraints.maxHeight.toFloat() / rows
        val spacing = prefs.desktopSpacing.dp
        val labelSpace = if (prefs.showDesktopLabels) 22.dp else 6.dp
        val iconSize = (52.dp * prefs.desktopIconScale)
            .coerceAtMost(maxHeight / rows - spacing - labelSpace)
            .coerceAtLeast(24.dp)
        val accent = LocalAccent.current

        /** The slot under the dragged icon's centre, or -1 when it is outside this page. */
        fun targetSlot(from: Int): Int {
            val local = from - page * perPage
            if (local < 0) return -1
            val centerX = (local % cols + 0.5f) * cellWidthPx + dragOffset.x
            val centerY = (local / cols + 0.5f) * cellHeightPx + dragOffset.y
            val col = floor(centerX / cellWidthPx).toInt()
            val row = floor(centerY / cellHeightPx).toInt()
            if (col !in 0 until cols || row !in 0 until rows) return -1
            return page * perPage + row * cols + col
        }

        val hoverSlot = if (dragSlot >= 0) targetSlot(dragSlot) else -1

        Column(Modifier.fillMaxSize()) {
            for (row in 0 until rows) {
                // Keep the row holding the dragged icon above the rows below it
                val dragRow = if (dragSlot >= page * perPage) (dragSlot - page * perPage) / cols else -1
                Row(Modifier.weight(1f).fillMaxWidth().zIndex(if (dragRow == row) 1f else 0f)) {
                    for (col in 0 until cols) {
                        val slot = page * perPage + row * cols + col
                        val item = bySlot[slot]
                        val isDragged = slot == dragSlot
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(spacing / 2)
                                .zIndex(if (isDragged) 1f else 0f)
                                .then(
                                    if (editMode && hoverSlot == slot && !isDragged) {
                                        Modifier.border(2.dp, accent, RoundedCornerShape(16.dp))
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item == null) {
                                if (!editMode) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {},
                                                onLongClick = onEnterEdit,
                                            ),
                                    )
                                }
                            } else {
                                val dragModifier = if (editMode) {
                                    Modifier.pointerInput(slot, cols, rows, page, cellWidthPx, cellHeightPx) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragSlot = slot
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                            },
                                            onDragEnd = {
                                                val target = targetSlot(slot)
                                                if (target >= 0) {
                                                    prefs.updateHomeItems(HomeLayout.drop(prefs.homeItems, slot, target))
                                                }
                                                dragSlot = -1
                                                dragOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                dragSlot = -1
                                                dragOffset = Offset.Zero
                                            },
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                                Box(
                                    dragModifier.graphicsLayer {
                                        if (isDragged) {
                                            translationX = dragOffset.x
                                            translationY = dragOffset.y
                                            scaleX = 1.1f
                                            scaleY = 1.1f
                                        }
                                    },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (item) {
                                        is HomeApp -> installed[item.packageName]?.let { app ->
                                            ShortcutCell(prefs, app, iconSize, editMode, onEnterEdit, onRename)
                                        }
                                        is HomeFolder -> FolderCell(prefs, item, installed, iconSize, editMode, onEnterEdit, onOpenFolder)
                                    }
                                    if (editMode) {
                                        RemoveBadge(
                                            description = "Remove from Home",
                                            modifier = Modifier.align(Alignment.TopStart),
                                        ) {
                                            when (item) {
                                                is HomeApp -> removeFromHome(prefs, item.packageName)
                                                is HomeFolder -> prefs.updateHomeItems(
                                                    HomeLayout.deleteFolder(
                                                        prefs.homeItems,
                                                        item.id,
                                                        HomeLayout.maxSlots(perPage),
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoveBadge(description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFFDF3F4A))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutCell(
    prefs: LauncherPrefs,
    app: AppInfo,
    iconSize: Dp,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onRename: (AppInfo) -> Unit,
) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    var menu by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val name = labelFor(prefs, app)

    Box {
        Column(
            Modifier
                .pressScale(source, 1.07f)
                .onFocusChanged { focused = it.isFocused }
                .then(if (focused) Modifier.border(2.dp, accent, RoundedCornerShape(14.dp)) else Modifier)
                .combinedClickable(
                    enabled = !editMode,
                    interactionSource = source,
                    indication = null,
                    onClick = { launchApp(context, app) },
                    onLongClick = { menu = true },
                )
                .semantics { contentDescription = name }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AppIconImage(app, iconSize)
            if (prefs.showDesktopLabels) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = LabelStyle,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Rename") },
                onClick = {
                    menu = false
                    onRename(app)
                },
            )
            DropdownMenuItem(
                text = { Text("Edit layout") },
                onClick = {
                    menu = false
                    onEnterEdit()
                },
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCell(
    prefs: LauncherPrefs,
    folder: HomeFolder,
    installed: Map<String, AppInfo>,
    iconSize: Dp,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(iconSize * 0.23f)
    val previews = folder.members.mapNotNull { installed[it] }.take(4)
    val perPage = homePerPage(prefs)

    Box {
        Column(
            Modifier
                .pressScale(source, 1.07f)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { onOpen(folder.id) },
                    onLongClick = { if (!editMode) menu = true },
                )
                .semantics { contentDescription = "Folder ${folder.name}" }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                Modifier
                    .size(iconSize)
                    .glass(shape, prefs.glassOpacity)
                    .padding(iconSize * 0.1f),
            ) {
                val mini = iconSize * 0.36f
                Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(iconSize * 0.04f)) {
                    for (r in 0 until 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(iconSize * 0.04f)) {
                            for (c in 0 until 2) {
                                val app = previews.getOrNull(r * 2 + c)
                                if (app != null) AppIconImage(app, mini) else Spacer(Modifier.size(mini))
                            }
                        }
                    }
                }
            }
            if (prefs.showDesktopLabels) {
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = LabelStyle,
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Open") },
                onClick = {
                    menu = false
                    onOpen(folder.id)
                },
            )
            DropdownMenuItem(
                text = { Text("Edit layout") },
                onClick = {
                    menu = false
                    onEnterEdit()
                },
            )
            // The apps go back onto the home screen, so nothing is lost
            DropdownMenuItem(
                text = { Text("Delete folder") },
                onClick = {
                    menu = false
                    prefs.updateHomeItems(HomeLayout.deleteFolder(prefs.homeItems, folder.id, HomeLayout.maxSlots(perPage)))
                },
            )
        }
    }
}

// ---- Dock ---------------------------------------------------------------------------------------

/** Shown instead of the dock while arranging the home screen. */
@Composable
private fun EditBar(prefs: LauncherPrefs, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .glass(RoundedCornerShape(26.dp), prefs.glassOpacity)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Drag icons to move them. Drop one on another to make a folder.",
            color = Color.White,
            fontSize = 13.sp,
            style = LabelStyle,
        )
        GlassButton("Done", primary = true, onClick = onDone)
    }
}

@Composable
private fun Dock(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    onGlass: () -> Unit,
    onRecents: () -> Unit,
    onAdd: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    otgConnected: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dock = dockApps(prefs, apps)
    val tile = (48 * prefs.dockScale).dp

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
                    .size(width = 4.dp, height = tile * 0.54f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
            )
        }

        // Mobile settings
        IconTile(tile, Tiles.gray, "Mobile settings", onClick = { openMobileSettings(context) }) {
            SvgIcon(Ico.gear, tile * 0.54f)
        }

        // Pinned apps: Chrome, YouTube, Google, GPT by default
        dock.forEachIndexed { index, app ->
            key(app.key) {
                DockApp(
                    app = app,
                    tile = tile,
                    canMoveLeft = index > 0,
                    canMoveRight = index < dock.size - 1,
                    onMove = { delta -> moveInDock(prefs, apps, app, delta) },
                    onRemove = { removeFromDock(prefs, apps, app) },
                )
            }
        }

        // OTG shortcut: opens Android's storage / USB drive screen (with fallbacks), not our Settings
        if (prefs.otgEnabled && otgConnected) {
            IconTile(tile, Tiles.green, "USB / OTG storage", onClick = { openOtgDestination(context) }) {
                SvgIcon(Ico.usb, tile * 0.54f)
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
            SvgIcon(Ico.plus, tile * 0.5f, strokeWidth = 2.2f)
        }

        // App drawer
        IconTile(tile, Tiles.blue, "App drawer", onClick = onOpenDrawer) {
            SvgIcon(Ico.grid, tile * 0.54f)
        }

        Box(
            Modifier
                .size(width = 1.dp, height = tile * 0.8f)
                .background(Color.White.copy(alpha = 0.45f)),
        )

        // App settings
        IconTile(tile, Tiles.graphite, "App settings", onClick = onOpenSettings) {
            SvgIcon(Ico.sliders, tile * 0.54f)
        }

        // Exit / switch launcher
        IconTile(tile, Tiles.red, "Exit launcher", onClick = { openLauncherChooser(context) }) {
            SvgIcon(Ico.exit, tile * 0.54f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockApp(
    app: AppInfo,
    tile: Dp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    var menu by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }

    Box {
        Box(
            Modifier
                .size(tile)
                .pressScale(source)
                .onFocusChanged { focused = it.isFocused }
                .then(if (focused) Modifier.border(2.dp, accent, RoundedCornerShape(tile * 0.24f)) else Modifier)
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
                    onClick = {
                        menu = false
                        onMove(-1)
                    },
                )
            }
            if (canMoveRight) {
                DropdownMenuItem(
                    text = { Text("Move right") },
                    onClick = {
                        menu = false
                        onMove(1)
                    },
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
