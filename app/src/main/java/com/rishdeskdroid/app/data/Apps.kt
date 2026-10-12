package com.rishdeskdroid.app.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/** One launchable app. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val key: String get() = "$packageName/$activityName"
    val component: ComponentName get() = ComponentName(packageName, activityName)
}

/** Default dock apps, in the order from the design. Only the ones installed are shown. */
val DEFAULT_DOCK = listOf(
    "com.android.chrome",
    "com.google.android.youtube",
    "com.google.android.googlequicksearchbox",
    "com.openai.chatgpt",
)

const val MAX_DOCK_APPS = 6

/** Loads every app that has a launcher icon. */
class AppRepository(private val context: Context) {

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val reloadGeneration = AtomicInteger(0)
    private val queryExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "RishDeskdroid-AppQuery").apply { isDaemon = true }
    }

    /**
     * Serialize package-manager queries and ignore stale results when a newer refresh was requested.
     * A temporary query failure must not erase the last usable application list.
     */
    fun reload() {
        val generation = reloadGeneration.incrementAndGet()
        queryExecutor.execute {
            val result = runCatching { query() }
            handler.post {
                if (generation == reloadGeneration.get() && result.isSuccess) {
                    IconLoader.clear()
                    apps = result.getOrThrow()
                }
            }
        }
    }

    fun close() {
        reloadGeneration.incrementAndGet()
        queryExecutor.shutdownNow()
    }

    private fun query(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }
}

/** The apps currently pinned in the dock (resolved against what is installed). */
fun dockApps(prefs: LauncherPrefs, apps: List<AppInfo>): List<AppInfo> =
    (prefs.dockPackages ?: DEFAULT_DOCK).mapNotNull { pkg ->
        apps.firstOrNull { it.packageName == pkg }
    }

/** Returns false if the dock is full. */
fun addToDock(prefs: LauncherPrefs, apps: List<AppInfo>, app: AppInfo): Boolean {
    val current = dockApps(prefs, apps).map { it.packageName }
    if (app.packageName in current) return true
    if (current.size >= MAX_DOCK_APPS) return false
    prefs.updateDockPackages(current + app.packageName)
    return true
}

fun removeFromDock(prefs: LauncherPrefs, apps: List<AppInfo>, app: AppInfo) {
    val current = dockApps(prefs, apps).map { it.packageName }
    prefs.updateDockPackages(current - app.packageName)
}

const val HOME_COLUMNS = 5
const val HOME_ROWS = 4
const val HOME_PER_PAGE = HOME_COLUMNS * HOME_ROWS
const val MAX_HOME_PAGES = 10

enum class AddToHomeResult { Added, AlreadyOnHome, Unavailable, Full }

/** Puts the app in the first free home-screen cell (a new page opens when a page is full). */
fun addToHome(prefs: LauncherPrefs, apps: List<AppInfo>, app: AppInfo): AddToHomeResult {
    if (apps.none { it.packageName == app.packageName }) return AddToHomeResult.Unavailable
    val current = prefs.homeShortcuts
    if (current.any { it.packageName == app.packageName }) return AddToHomeResult.AlreadyOnHome
    val used = current.map { it.slot }.toSet()
    val slot = (0 until HOME_PER_PAGE * MAX_HOME_PAGES).firstOrNull { it !in used }
        ?: return AddToHomeResult.Full
    prefs.updateHomeShortcuts(current + HomeShortcut(slot, app.packageName))
    return AddToHomeResult.Added
}

/**
 * Reorders a home shortcut by one grid cell. Horizontal moves never wrap between rows;
 * vertical moves stay within the same page. If the destination is occupied, the shortcuts swap.
 * Invalid directions, missing apps, and boundary moves leave the list unchanged.
 */
fun reorderHomeShortcuts(
    shortcuts: List<HomeShortcut>,
    packageName: String,
    delta: Int,
): List<HomeShortcut> {
    if (delta !in setOf(-HOME_COLUMNS, -1, 1, HOME_COLUMNS)) return shortcuts
    val source = shortcuts.firstOrNull { it.packageName == packageName } ?: return shortcuts
    val from = source.slot
    val target = from + delta
    if (target !in 0 until HOME_PER_PAGE * MAX_HOME_PAGES) return shortcuts
    if (delta == -1 && from % HOME_COLUMNS == 0) return shortcuts
    if (delta == 1 && from % HOME_COLUMNS == HOME_COLUMNS - 1) return shortcuts
    if (delta == -HOME_COLUMNS && from % HOME_PER_PAGE / HOME_COLUMNS == 0) return shortcuts
    if (delta == HOME_COLUMNS && from % HOME_PER_PAGE / HOME_COLUMNS == HOME_ROWS - 1) return shortcuts

    val occupant = shortcuts.firstOrNull { it.slot == target }
    return shortcuts.map { shortcut ->
        when {
            shortcut.packageName == packageName -> shortcut.copy(slot = target)
            occupant != null && shortcut.packageName == occupant.packageName -> shortcut.copy(slot = from)
            else -> shortcut
        }
    }.sortedBy { it.slot }
}

/** Persist a shortcut move. Returns false when the move was not possible. */
fun moveHomeShortcut(prefs: LauncherPrefs, packageName: String, delta: Int): Boolean {
    val current = prefs.homeShortcuts
    val reordered = reorderHomeShortcuts(current, packageName, delta)
    if (reordered == current) return false
    prefs.updateHomeShortcuts(reordered)
    return true
}

/** Removes only the home shortcut. The app stays installed and stays in the drawer. */
fun removeFromHome(prefs: LauncherPrefs, packageName: String) {
    prefs.updateHomeShortcuts(prefs.homeShortcuts.filter { it.packageName != packageName })
}

/** Forgets shortcuts whose app is no longer installed. */
fun pruneHomeShortcuts(prefs: LauncherPrefs, pm: PackageManager) {
    val kept = prefs.homeShortcuts.filter {
        try {
            pm.getApplicationInfo(it.packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    if (kept.size != prefs.homeShortcuts.size) prefs.updateHomeShortcuts(kept)
}

/**
 * Pure app-drawer filtering and sorting logic, separated from Compose so it can be tested.
 * When showHidden is true, only hidden apps are shown to make restoring them unambiguous.
 */
fun filterAndSortApps(
    apps: List<AppInfo>,
    query: String,
    hiddenPackages: Set<String>,
    showHidden: Boolean,
    favoritePackages: Set<String>,
    sort: String,
): List<AppInfo> {
    val normalized = query.trim()
    val base = apps.asSequence()
        .filter { if (showHidden) it.packageName in hiddenPackages else it.packageName !in hiddenPackages }
        .filter {
            normalized.isEmpty() ||
                it.label.contains(normalized, ignoreCase = true) ||
                it.packageName.contains(normalized, ignoreCase = true)
        }
        .toList()

    return when (sort) {
        LauncherPrefs.SORT_NAME_DESC -> base.sortedWith(
            compareByDescending<AppInfo> { it.label.lowercase() }.thenBy { it.packageName },
        )
        LauncherPrefs.SORT_FAVORITES -> base.sortedWith(
            compareByDescending<AppInfo> { it.packageName in favoritePackages }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName },
        )
        else -> base.sortedWith(compareBy<AppInfo> { it.label.lowercase() }.thenBy { it.packageName })
    }
}

class LoadedIcon(val bitmap: ImageBitmap, val adaptive: Boolean)

/**
 * Renders app icons to bitmaps. Adaptive icons are drawn as their full 108dp layers and cropped
 * to the visible 72dp area so the UI can clip them to the launcher's own rounded-square shape.
 */
object IconLoader {
    private val cache = ConcurrentHashMap<String, LoadedIcon>()

    fun peek(app: AppInfo): LoadedIcon? = cache[app.key]

    fun clear() = cache.clear()

    fun load(pm: PackageManager, app: AppInfo): LoadedIcon {
        cache[app.key]?.let { return it }

        val drawable = try {
            pm.getActivityIcon(app.component)
        } catch (e: Exception) {
            pm.defaultActivityIcon
        }

        val px = 192
        val bitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var adaptive = false

        if (drawable is AdaptiveIconDrawable) {
            adaptive = true
            val full = px * 108 / 72
            val offset = (full - px) / 2
            canvas.translate(-offset.toFloat(), -offset.toFloat())
            drawable.background?.let {
                it.setBounds(0, 0, full, full)
                it.draw(canvas)
            }
            drawable.foreground?.let {
                it.setBounds(0, 0, full, full)
                it.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, px, px)
            drawable.draw(canvas)
        }

        val loaded = LoadedIcon(bitmap.asImageBitmap(), adaptive)
        cache[app.key] = loaded
        return loaded
    }
}

private fun safeStart(context: Context, intent: Intent, errorMessage: String) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Launches an app without changing the device-wide rotation settings.
 *
 * Android does not provide a public launcher API to force an unrelated app's activity
 * into landscape. Each target app and the device's own orientation settings control that.
 */
fun launchApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    safeStart(context, intent, "Can't open ${app.label}")
}

fun openAppInfo(context: Context, app: AppInfo) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
    safeStart(context, intent, "Can't open app info")
}

fun uninstallApp(context: Context, app: AppInfo) {
    val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
    safeStart(context, intent, "Can't uninstall ${app.label}")
}

fun openMobileSettings(context: Context) {
    safeStart(context, Intent(Settings.ACTION_SETTINGS), "Can't open settings")
}

/** The "Exit" tile: opens Android's default-home picker so the user can switch launcher. */
fun openLauncherChooser(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        openMobileSettings(context)
    }
}

/**
 * The OTG tile. Android has no public "OTG settings" page, so this tries the system storage
 * screens (where a USB drive is listed and can be opened or ejected) and only then falls back to
 * the system file picker. Each step is checked before launching and failures are handled.
 */
fun openOtgDestination(context: Context) {
    val pm = context.packageManager
    val candidates = listOf(
        Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        Intent(Settings.ACTION_MEMORY_CARD_SETTINGS),
    )
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(pm) == null) continue
        try {
            context.startActivity(intent)
            return
        } catch (e: ActivityNotFoundException) {
            // try the next destination
        } catch (e: SecurityException) {
            // try the next destination
        }
    }
    openOtgStorage(context)
}

/** The OTG tile fallback: opens the system file picker, where a connected USB drive appears in the sidebar. */
fun openOtgStorage(context: Context) {
    safeStart(context, Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), "No file manager available")
}
