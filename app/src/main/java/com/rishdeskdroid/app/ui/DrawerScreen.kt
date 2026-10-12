package com.rishdeskdroid.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.data.AddToHomeResult
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.addToDock
import com.rishdeskdroid.app.data.addToHome
import com.rishdeskdroid.app.data.launchApp
import com.rishdeskdroid.app.data.filterAndSortApps
import com.rishdeskdroid.app.data.openAppInfo
import com.rishdeskdroid.app.data.uninstallApp

private const val COLUMNS = 5
private const val ROWS = 3
private const val PER_PAGE = COLUMNS * ROWS

/**
 * The app drawer supports live search, persistent sorting, favorites, and reversible hiding.
 * Long-press an app to manage it; hidden apps remain installed and can be restored with Show hidden.
 */
@Composable
fun DrawerScreen(
    prefs: LauncherPrefs,
    apps: List<AppInfo>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val visibleApps = remember(apps, query, showHidden, prefs.hiddenPackages, prefs.favoritePackages, prefs.appSort) {
        filterAndSortApps(
            apps = apps,
            query = query,
            hiddenPackages = prefs.hiddenPackages,
            showHidden = showHidden,
            favoritePackages = prefs.favoritePackages,
            sort = prefs.appSort,
        )
    }
    val pages = remember(visibleApps) {
        val chunks = visibleApps.chunked(PER_PAGE)
        if (chunks.isEmpty()) listOf(emptyList<AppInfo>()) else chunks
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (query.isEmpty()) {
                    Text("Search apps", color = Color.White.copy(alpha = 0.62f), fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search applications" },
                )
            }
            Box {
                DrawerActionButton(
                    label = when (prefs.appSort) {
                        LauncherPrefs.SORT_NAME_DESC -> "Z–A"
                        LauncherPrefs.SORT_FAVORITES -> "★"
                        else -> "A–Z"
                    },
                    onClick = { showSortMenu = true },
                )
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("Name: A–Z") }, onClick = {
                        prefs.updateAppSort(LauncherPrefs.SORT_NAME_ASC); showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text("Name: Z–A") }, onClick = {
                        prefs.updateAppSort(LauncherPrefs.SORT_NAME_DESC); showSortMenu = false
                    })
                    DropdownMenuItem(text = { Text("Favorites first") }, onClick = {
                        prefs.updateAppSort(LauncherPrefs.SORT_FAVORITES); showSortMenu = false
                    })
                }
            }
            DrawerActionButton(
                label = if (showHidden) "Hide hidden" else "Show hidden",
                onClick = { showHidden = !showHidden },
            )
        }

        if (visibleApps.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    if (query.isNotBlank()) "No apps found for “$query”"
                    else if (showHidden) "No hidden apps"
                    else "No apps available",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                AppPage(prefs, apps, pages[page], showHidden)
            }
            PageDots(count = pages.size, current = pagerState.currentPage)
        }
    }
}

@Composable
private fun DrawerActionButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun AppPage(prefs: LauncherPrefs, allApps: List<AppInfo>, items: List<AppInfo>, showHidden: Boolean) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // Size icons against both row height and column width so the grid remains usable on
        // short landscape screens and narrow devices instead of clipping labels or touch targets.
        val iconSize = minOf(
            54.dp,
            (maxHeight / ROWS - 24.dp).coerceAtLeast(28.dp),
            (maxWidth / COLUMNS - 10.dp).coerceAtLeast(28.dp),
        )
        Column(Modifier.fillMaxSize()) {
            for (row in 0 until ROWS) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until COLUMNS) {
                        val app = items.getOrNull(row * COLUMNS + col)
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (app != null) DrawerApp(prefs, allApps, app, showHidden, iconSize)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerApp(prefs: LauncherPrefs, allApps: List<AppInfo>, app: AppInfo, showHidden: Boolean, iconSize: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val favorite = app.packageName in prefs.favoritePackages
    val hidden = app.packageName in prefs.hiddenPackages

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
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                AppIconImage(app, iconSize)
                if (favorite) {
                    Text("★", color = Color(0xFFFFD76A), fontSize = 13.sp)
                }
            }
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
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (favorite) "Remove from Favorites" else "Add to Favorites") },
                onClick = { prefs.toggleFavorite(app.packageName); menu = false },
            )
            DropdownMenuItem(
                text = { Text(if (hidden) "Restore to Drawer" else "Hide from Drawer") },
                onClick = {
                    if (hidden) prefs.unhidePackage(app.packageName) else prefs.hidePackage(app.packageName)
                    menu = false
                },
            )
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
                text = { Text("Add to dock") },
                onClick = {
                    menu = false
                    if (!addToDock(prefs, allApps, app)) {
                        Toast.makeText(context, "The dock is full", Toast.LENGTH_SHORT).show()
                    }
                },
            )
            DropdownMenuItem(text = { Text("App info") }, onClick = {
                menu = false
                openAppInfo(context, app)
            })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = {
                menu = false
                uninstallApp(context, app)
            })
        }
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
