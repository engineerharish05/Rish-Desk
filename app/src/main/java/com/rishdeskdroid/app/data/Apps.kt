package com.rishdeskdroid.app.data

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.format.Formatter
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private const val PREFS_NAME = "glass_launcher"

/** One launchable app. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val installTime: Long = 0L,
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

const val MAX_DOCK_APPS = 8

/** Loads every app that has a launcher icon. */
class AppRepository(private val context: Context) {

    var apps by mutableStateOf<List<AppInfo>>(emptyList())
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger(0)
    private val delayedReload = Runnable { reload() }

    /** Reload after a short pause, so a burst of install/update broadcasts causes one reload. */
    fun reloadSoon() {
        handler.removeCallbacks(delayedReload)
        handler.postDelayed(delayedReload, 300)
    }

    fun reload() {
        val id = generation.incrementAndGet()
        Thread {
            val list = try {
                query()
            } catch (e: Exception) {
                null
            }
            if (list != null) {
                handler.post {
                    // Ignore the result if a newer reload was started in the meantime
                    if (id == generation.get() && list != apps) apps = list
                }
            }
        }.start()
    }

    fun close() {
        handler.removeCallbacksAndMessages(null)
        generation.incrementAndGet()
    }

    private fun query(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val resolved = pm.queryIntentActivities(intent, 0)
        val installTimes = HashMap<String, Long>()
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                val pkg = it.activityInfo.packageName
                val installed = installTimes.getOrPut(pkg) {
                    try {
                        pm.getPackageInfo(pkg, 0).firstInstallTime
                    } catch (e: Exception) {
                        0L
                    }
                }
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = pkg,
                    activityName = it.activityInfo.name,
                    installTime = installed,
                )
            }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }
}

// ---- Dock ---------------------------------------------------------------------------------------

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

/** Moves a dock app one place left (-1) or right (+1). */
fun moveInDock(prefs: LauncherPrefs, apps: List<AppInfo>, app: AppInfo, delta: Int) {
    val list = dockApps(prefs, apps).map { it.packageName }.toMutableList()
    val from = list.indexOf(app.packageName)
    val to = from + delta
    if (from < 0 || to !in list.indices) return
    Collections.swap(list, from, to)
    prefs.updateDockPackages(list)
}

// ---- Recently used (launches made from this launcher) ---------------------------------------------

/**
 * Android only reports other apps' usage to apps holding the special "usage access" permission.
 * To avoid asking for it, the launcher remembers the apps opened from itself.
 */
object RecentStore {
    private const val MAX = 12
    private const val KEY = "recent_apps"

    var recents by mutableStateOf<List<String>>(emptyList())
        private set

    private var loaded = false

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        recents = sp(context).getString(KEY, "").orEmpty().split("|").filter { it.isNotBlank() }.distinct().take(MAX)
    }

    fun record(context: Context, packageName: String) {
        ensureLoaded(context)
        recents = (listOf(packageName) + recents.filter { it != packageName }).take(MAX)
        sp(context).edit().putString(KEY, recents.joinToString("|")).apply()
    }

    fun clear(context: Context) {
        ensureLoaded(context)
        recents = emptyList()
        sp(context).edit().remove(KEY).apply()
    }
}

// ---- Icons --------------------------------------------------------------------------------------

class LoadedIcon(val bitmap: ImageBitmap, val adaptive: Boolean)

/**
 * Renders app icons to bitmaps. Adaptive icons are drawn as their full 108dp layers and cropped
 * to the visible 72dp area so the UI can clip them to the launcher's own rounded-square shape.
 */
object IconLoader {
    private val cache = ConcurrentHashMap<String, LoadedIcon>()

    /** Changes whenever cached icons are dropped, so visible icons reload. */
    var version by mutableIntStateOf(0)
        private set

    fun peek(app: AppInfo): LoadedIcon? = cache[app.key]

    /** Forget the icons of one app (after it was installed, updated or removed), or all of them. */
    fun invalidate(packageName: String?) {
        if (packageName == null) {
            cache.clear()
        } else {
            val prefix = "$packageName/"
            cache.keys.removeIf { it.startsWith(prefix) }
        }
        version += 1
    }

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

        try {
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
        } catch (e: Exception) {
            // Leaves a blank icon rather than crashing; the label still identifies the app
        }

        val loaded = LoadedIcon(bitmap.asImageBitmap(), adaptive)
        cache[app.key] = loaded
        return loaded
    }
}

// ---- Launching ----------------------------------------------------------------------------------

private fun tryStart(context: Context, intent: Intent, options: android.os.Bundle? = null): Boolean =
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), options)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

private fun safeStart(context: Context, intent: Intent, errorMessage: String) {
    if (!tryStart(context, intent)) Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
}

private var lastLaunchPackage: String? = null
private var lastLaunchAt = 0L

/**
 * Opens the app. With "Open apps in landscape" on and the "Modify system settings" permission
 * granted, the screen is first locked to landscape (and put back when the user returns here).
 * Apps that insist on portrait can still stay portrait; Android gives launchers no way to force them.
 */
fun launchApp(context: Context, app: AppInfo) {
    // Ignore a second tap on the same app right after the first
    val now = SystemClock.elapsedRealtime()
    if (app.packageName == lastLaunchPackage && now - lastLaunchAt < 800) return
    lastLaunchPackage = app.packageName
    lastLaunchAt = now

    val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var locked = false
    if (sp.getBoolean("landscape_apps", true)) {
        locked = OrientationGuard.lock(context)
        if (!locked && !sp.getBoolean("asked_write_settings", false)) {
            sp.edit().putBoolean("asked_write_settings", true).apply()
            Toast.makeText(
                context,
                "To open apps in landscape, allow \"Modify system settings\" in RishDesk settings",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(app.component)
        .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    val started = tryStart(context, intent, windowedOptions(context, sp.getBoolean("windowed_apps", false)))
    if (started) {
        RecentStore.record(context, app.packageName)
    } else {
        if (locked) OrientationGuard.restore(context)
        Toast.makeText(context, "Can't open ${app.label}", Toast.LENGTH_SHORT).show()
    }
}

/** True when this device can show apps in floating windows (freeform mode). */
fun supportsFreeform(context: Context): Boolean =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT)

/** Launch bounds for a floating window, only when the user asked for it and the device supports it. */
private fun windowedOptions(context: Context, enabled: Boolean): android.os.Bundle? {
    if (!enabled || !supportsFreeform(context)) return null
    return try {
        val dm = context.resources.displayMetrics
        val w = (dm.widthPixels * 0.7f).toInt()
        val h = (dm.heightPixels * 0.8f).toInt()
        val left = (dm.widthPixels - w) / 2
        val top = (dm.heightPixels - h) / 2
        ActivityOptions.makeBasic().setLaunchBounds(Rect(left, top, left + w, top + h)).toBundle()
    } catch (e: Exception) {
        null
    }
}

/**
 * Remembers and restores the user's rotation settings around landscape app launches, so the
 * launcher never leaves auto-rotate switched off.
 */
object OrientationGuard {
    private const val K_ACTIVE = "rotation_lock_active"
    private const val K_ACCEL = "rotation_prev_accelerometer"
    private const val K_USER = "rotation_prev_user"

    private fun sp(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    fun lock(context: Context): Boolean {
        if (!Settings.System.canWrite(context)) return false
        return try {
            val resolver = context.contentResolver
            val store = sp(context)
            if (!store.getBoolean(K_ACTIVE, false)) {
                store.edit()
                    .putInt(K_ACCEL, Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1))
                    .putInt(K_USER, Settings.System.getInt(resolver, Settings.System.USER_ROTATION, Surface.ROTATION_0))
                    .putBoolean(K_ACTIVE, true)
                    .apply()
            }
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val rotation =
                if (wm.defaultDisplay.rotation == Surface.ROTATION_270) Surface.ROTATION_270 else Surface.ROTATION_90
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, rotation)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Puts the user's own rotation settings back, if this launcher changed them. */
    fun restore(context: Context) {
        val store = sp(context)
        if (!store.getBoolean(K_ACTIVE, false)) return
        try {
            if (Settings.System.canWrite(context)) {
                val resolver = context.contentResolver
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, store.getInt(K_USER, Surface.ROTATION_0))
                Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, store.getInt(K_ACCEL, 1))
            }
        } catch (e: Exception) {
            // Nothing more can be done; the flag is cleared below so we do not retry forever
        }
        store.edit().putBoolean(K_ACTIVE, false).apply()
    }
}

// ---- System shortcuts ---------------------------------------------------------------------------

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

fun openDisplaySettings(context: Context) =
    safeStart(context, Intent(Settings.ACTION_DISPLAY_SETTINGS), "Can't open display settings")

fun openWifiSettings(context: Context) =
    safeStart(context, Intent(Settings.ACTION_WIFI_SETTINGS), "Can't open Wi-Fi settings")

fun openBluetoothSettings(context: Context) =
    safeStart(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Can't open Bluetooth settings")

fun openBatterySettings(context: Context) =
    safeStart(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), "Can't open battery settings")

fun openNotificationSettings(context: Context) {
    val intent = Intent("android.settings.ALL_APPS_NOTIFICATION_SETTINGS")
    if (!tryStart(context, intent)) openMobileSettings(context)
}

fun openDeveloperSettings(context: Context) {
    if (!tryStart(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))) {
        Toast.makeText(
            context,
            "Developer options are not enabled. Tap the build number in About phone seven times.",
            Toast.LENGTH_LONG,
        ).show()
    }
}

fun openStorageSettings(context: Context) {
    if (!tryStart(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))) openMobileSettings(context)
}

/** Lets the user grant "Modify system settings", which is what allows landscape locking. */
fun openWriteSettings(context: Context) {
    safeStart(
        context,
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")),
        "Can't open settings",
    )
}

fun canWriteSettings(context: Context): Boolean = Settings.System.canWrite(context)

/** The "Exit" tile: opens Android's default-home picker so the user can switch launcher. */
fun openLauncherChooser(context: Context) {
    if (!tryStart(context, Intent(Settings.ACTION_HOME_SETTINGS))) openMobileSettings(context)
}

/**
 * The OTG tile. Android has no public "OTG settings" page, so this tries the system storage
 * screens (where a USB drive is listed and can be opened or ejected) and only then falls back to
 * the system file browser. Each step is checked before launching and failures are handled.
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
        if (tryStart(context, intent)) return
    }
    openFiles(context)
}

/** Opens Android's own file browser (Files / Documents) at the device storage root. */
fun openFiles(context: Context) {
    val browse = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse("content://com.android.externalstorage.documents/root/primary"), "vnd.android.document/root")
    if (tryStart(context, browse)) return
    safeStart(context, Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), "No file manager available")
}

private val TERMINAL_PACKAGES = listOf(
    "com.termux",
    "com.termux.nightly",
    "jackpal.androidterm",
    "com.offsec.nhterm",
)

/** An installed terminal app, if there is one. */
fun terminalApp(apps: List<AppInfo>): AppInfo? =
    TERMINAL_PACKAGES.firstNotNullOfOrNull { pkg -> apps.firstOrNull { it.packageName == pkg } }

/** "12.3 GB free of 64 GB", or null when Android does not report it. */
fun storageSummary(context: Context): String? = try {
    val stat = StatFs(Environment.getDataDirectory().path)
    val total = stat.blockCountLong * stat.blockSizeLong
    val free = stat.availableBlocksLong * stat.blockSizeLong
    "${Formatter.formatFileSize(context, free)} free of ${Formatter.formatFileSize(context, total)}"
} catch (e: Exception) {
    null
}
