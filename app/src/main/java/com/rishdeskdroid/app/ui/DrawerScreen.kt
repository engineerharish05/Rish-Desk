package com.rishdeskdroid.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.data.AddToHomeResult
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.DrawerFilter
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.RecentStore
import com.rishdeskdroid.app.data.SortMode
import com.rishdeskdroid.app.data.addToDock
import com.rishdeskdroid.app.data.addToHome
import com.rishdeskdroid.app.data.canHide
import com.rishdeskdroid.app.data.dockApps
import com.rishdeskdroid.app.data.drawerApps
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.openAppInfo
import com.rishdeskdroid.app.data.removeFromDock
import com.rishdeskdroid.app.data.uninstallApp
import androidx.compose.runtime.withFrameNanos

/**
 * The app drawer: search, filters (All / Favorites / Recent), sort order, and a paged grid whose
 * size comes from Settings. Tap empty space (or press back) to close it.
 */
@Composable
fun DrawerScreen(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    focusSearch: Boolean,
    onSearchFocusHandled: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var filterIndex by rememberSaveable { mutableStateOf(0) }
    val filter = DrawerFilter.entries[filterIndex.coerceIn(0, DrawerFilter.entries.size - 1)]
    val focusRequester = remember { FocusRequester() }

    val columns = prefs.drawerColumns
    val rows = prefs.drawerRows
    val perPage = columns * rows

    val list = remember(apps, query, filter, prefs.sortMode, prefs.hiddenApps, prefs.favorites, RecentStore.recents) {
        drawerApps(prefs, apps, filter, query)
    }
    val pages = remember(list, perPage) { list.chunked(perPage) }
    val pagerState = rememberPagerState(pageCount = { maxOf(1, pages.size) })

    // Back clears the search first, then closes the drawer
    BackHandler(enabled = query.isNotEmpty()) {
        query = ""
        focusManager.clearFocus()
    }

    LaunchedEffect(query, filterIndex, prefs.sortMode) { pagerState.scrollToPage(0) }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // The field was not on screen yet; the user can tap it
            }
            onSearchFocusHandled()
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Close app drawer",
                onClick = onClose,
            ),
    ) {
        DrawerHeader(
            prefs = prefs,
            query = query,
            onQueryChange = { query = it },
            filter = filter,
            onFilter = { filterIndex = it.ordinal },
            focusRequester = focusRequester,
            onSubmit = {
                // Enter launches the best match
                val first = list.firstOrNull()
                if (query.isNotBlank() && first != null) {
                    launchApp(context, first)
                    query = ""
                }
                focusManager.clearFocus()
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            if (list.isEmpty()) {
                EmptyDrawer(query, filter, apps.isEmpty())
            } else {
                AppPage(prefs, apps, pages.getOrNull(page).orEmpty(), columns, rows)
            }
        }
        if (pages.size > 1) PageDots(count = pages.size, current = pagerState.currentPage)
    }
}

@Composable
private fun DrawerHeader(
    prefs: LauncherPrefs,
    query: String,
    onQueryChange: (String) -> Unit,
    filter: DrawerFilter,
    onFilter: (DrawerFilter) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GlassTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search apps",
            modifier = Modifier.weight(1f),
            focusRequester = focusRequester,
            imeAction = ImeAction.Search,
            onAction = onSubmit,
            trailing = if (query.isNotEmpty()) {
                @Composable {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onQueryChange("") }
                            .semantics { contentDescription = "Clear search" },
                        contentAlignment = Alignment.Center,
                    ) {
                        SvgIcon(Ico.close, 16.dp, strokeWidth = 2.4f)
                    }
                }
            } else {
                null
            },
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrawerFilter.entries.forEach { option ->
                Chip(option.label, selected = option == filter) { onFilter(option) }
            }
            SortChip(prefs)
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) accent else Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = if (selected) 0f else 0.34f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SortChip(prefs: LauncherPrefs) {
    var open by remember { mutableStateOf(false) }
    Box {
        Chip("Sort: ${prefs.sortMode.label}", selected = false) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(if (mode == prefs.sortMode) "✓ ${mode.label}" else mode.label) },
                    onClick = {
                        open = false
                        prefs.updateSortMode(mode)
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyDrawer(query: String, filter: DrawerFilter, noApps: Boolean) {
    val message = when {
        query.isNotBlank() -> "No apps match \"${query.trim()}\""
        filter == DrawerFilter.FAVORITES -> "No favorites yet. Long-press an app and choose Add to favorites."
        filter == DrawerFilter.RECENT -> "Apps you open from the launcher will appear here."
        noApps -> "No apps found"
        else -> "All apps are hidden. Restore them in Settings."
    }
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            style = LabelStyle,
        )
    }
}

@Composable
private fun AppPage(
    prefs: LauncherPrefs,
    allApps: List<AppInfo>,
    items: List<AppInfo>,
    columns: Int,
    rows: Int,
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 4.dp),
    ) {
        val cellHeight = maxHeight / rows
        val labelSpace = if (prefs.showDrawerLabels) 24.dp else 8.dp
        val icon = (54.dp * prefs.drawerIconScale).coerceAtMost(cellHeight - labelSpace).coerceAtLeast(24.dp)
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until rows) {
                Row(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    for (col in 0 until columns) {
                        val app = items.getOrNull(row * columns + col)
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app != null) DrawerApp(prefs, allApps, app, icon)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerApp(prefs: LauncherPrefs, allApps: List<AppInfo>, app: AppInfo, iconSize: Dp) {
    val context = LocalContext.current
    val accent = LocalAccent.current
    var menu by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val isFavorite = app.packageName in prefs.favorites
    val inDock = dockApps(prefs, allApps).any { it.packageName == app.packageName }
    val focusShape = RoundedCornerShape(14.dp)

    Box {
        Column(
            Modifier
                .pressScale(source, 1.07f)
                .onFocusChanged { focused = it.isFocused }
                .then(if (focused) Modifier.border(2.dp, accent, focusShape) else Modifier)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = { launchApp(context, app) },
                    onLongClick = { menu = true },
                )
                .semantics { contentDescription = app.label }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AppIconImage(app, iconSize)
            if (prefs.showDrawerLabels) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = LabelStyle,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Add to Home") },
                onClick = {
                    menu = false
                    val message = when (addToHome(prefs, allApps, app)) {
                        AddToHomeResult.Added -> "${app.label} added to Home"
                        AddToHomeResult.AlreadyOnHome -> "${app.label} is already on Home"
                        AddToHomeResult.Full -> "The home screen is full"
                        AddToHomeResult.Unavailable -> "Can't add ${app.label}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
            )
            DropdownMenuItem(
                text = { Text(if (inDock) "Remove from dock" else "Add to dock") },
                onClick = {
                    menu = false
                    if (inDock) {
                        removeFromDock(prefs, allApps, app)
                    } else if (!addToDock(prefs, allApps, app)) {
                        Toast.makeText(context, "The dock is full", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(if (isFavorite) "Remove from favorites" else "Add to favorites") },
                onClick = {
                    menu = false
                    prefs.toggleFavorite(app.packageName)
                },
            )
            if (canHide(app.packageName)) {
                DropdownMenuItem(
                    text = { Text("Hide app") },
                    onClick = {
                        menu = false
                        prefs.updateHidden(app.packageName, true)
                        Toast.makeText(context, "${app.label} hidden. Restore it in Settings.", Toast.LENGTH_SHORT).show()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("App info") },
                onClick = {
                    menu = false
                    openAppInfo(context, app)
                },
            )
            DropdownMenuItem(
                text = { Text("Uninstall") },
                onClick = {
                    menu = false
                    confirmUninstall = true
                },
            )
        }
    }

    if (confirmUninstall) {
        ConfirmDialog(
            title = "Uninstall ${app.label}?",
            message = "This removes the app from your device. To only remove a shortcut, use Remove from Home.",
            confirmLabel = "Uninstall",
            onConfirm = {
                confirmUninstall = false
                uninstallApp(context, app)
            },
            onDismiss = { confirmUninstall = false },
        )
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, top = 4.dp)
            .semantics { contentDescription = "Page ${current + 1} of $count" },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until count) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 1f else 0.4f)),
            )
        }
    }
}
