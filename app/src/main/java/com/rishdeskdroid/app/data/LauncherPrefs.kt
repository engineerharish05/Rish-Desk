package com.rishdeskdroid.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Keeps persisted or externally supplied opacity values finite and within the supported range.
 * Invalid floating-point values fall back to the launcher's default opacity.
 */
fun sanitizeGlassOpacity(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else 0.6f

/** One app shortcut on the home screen. */
data class HomeShortcut(val slot: Int, val packageName: String)

/**
 * All launcher settings, backed by SharedPreferences and exposed as Compose state so the UI
 * updates the moment a value changes.
 */
class LauncherPrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("glass_launcher", Context.MODE_PRIVATE)

    /** Settings > Widgets ON/OFF */
    var widgetsEnabled by mutableStateOf(sp.getBoolean(K_WIDGETS, true))
        private set

    /** Settings > OTG Shortcut ON/OFF */
    var otgEnabled by mutableStateOf(sp.getBoolean(K_OTG, true))
        private set

    /** Settings > Keyboard shortcuts ON/OFF */
    var keyboardEnabled by mutableStateOf(sp.getBoolean(K_KEYS, false))
        private set

    /** Settings > Glass Opacity, 0f..1f */
    var glassOpacity by mutableFloatStateOf(sanitizeGlassOpacity(sp.getFloat(K_GLASS, 0.6f)))
        private set

    /** Bumped whenever the wallpaper file changes so the UI reloads it. */
    var wallpaperVersion by mutableIntStateOf(0)
        private set

    /** AppWidget ids placed on the home screen. */
    var widgetIds by mutableStateOf(loadWidgetIds())
        private set

    /** Package names pinned in the dock, or null to use the default dock. */
    var dockPackages by mutableStateOf<List<String>?>(loadDock())
        private set

    /** App shortcuts on the home screen: package name plus grid slot (page * 20 + row * 5 + column). */
    var homeShortcuts by mutableStateOf(loadHomeShortcuts())
        private set

    /** Packages marked as favorites in the app drawer. */
    var favoritePackages by mutableStateOf(loadPackageSet(K_FAVORITES))
        private set

    /** Packages hidden from the normal app drawer (never uninstalled). */
    var hiddenPackages by mutableStateOf(loadPackageSet(K_HIDDEN))
        private set

    /** App drawer sorting: name_asc, name_desc, or favorites. */
    var appSort by mutableStateOf(sp.getString(K_APP_SORT, SORT_NAME_ASC) ?: SORT_NAME_ASC)
        private set

    fun updateWidgetsEnabled(value: Boolean) {
        widgetsEnabled = value
        sp.edit().putBoolean(K_WIDGETS, value).apply()
    }

    fun updateOtgEnabled(value: Boolean) {
        otgEnabled = value
        sp.edit().putBoolean(K_OTG, value).apply()
    }

    fun updateKeyboardEnabled(value: Boolean) {
        keyboardEnabled = value
        sp.edit().putBoolean(K_KEYS, value).apply()
    }

    fun updateGlassOpacity(value: Float) {
        val v = sanitizeGlassOpacity(value)
        glassOpacity = v
        sp.edit().putFloat(K_GLASS, v).apply()
    }

    fun bumpWallpaper() {
        wallpaperVersion += 1
    }

    fun updateWidgetIds(ids: List<Int>) {
        widgetIds = ids.distinct()
        sp.edit().putString(K_WIDGET_IDS, widgetIds.joinToString(",")).apply()
    }

    fun updateDockPackages(packages: List<String>) {
        dockPackages = packages.distinct()
        sp.edit().putString(K_DOCK, dockPackages!!.joinToString("|")).apply()
    }

    fun updateHomeShortcuts(list: List<HomeShortcut>) {
        val seenSlots = HashSet<Int>()
        val seenPackages = HashSet<String>()
        homeShortcuts = list.filter { it.slot >= 0 && it.slot < HOME_PER_PAGE * MAX_HOME_PAGES }
            .filter { seenSlots.add(it.slot) && seenPackages.add(it.packageName) }
        sp.edit().putString(K_HOME, homeShortcuts.joinToString("|") { "${it.slot}:${it.packageName}" }).apply()
    }

    fun toggleFavorite(packageName: String) {
        val next = favoritePackages.toMutableSet()
        if (!next.add(packageName)) next.remove(packageName)
        favoritePackages = next
        sp.edit().putString(K_FAVORITES, next.joinToString("|")).apply()
    }

    fun hidePackage(packageName: String) {
        val next = hiddenPackages + packageName
        hiddenPackages = next
        sp.edit().putString(K_HIDDEN, next.joinToString("|")).apply()
    }

    fun unhidePackage(packageName: String) {
        val next = hiddenPackages - packageName
        hiddenPackages = next
        sp.edit().putString(K_HIDDEN, next.joinToString("|")).apply()
    }

    fun updateAppSort(value: String) {
        appSort = value.takeIf { it in SORT_OPTIONS } ?: SORT_NAME_ASC
        sp.edit().putString(K_APP_SORT, appSort).apply()
    }

    private fun loadHomeShortcuts(): List<HomeShortcut> {
        val seenSlots = HashSet<Int>()
        val seenPackages = HashSet<String>()
        return sp.getString(K_HOME, "")
            .orEmpty()
            .split("|")
            .mapNotNull { entry ->
                val slot = entry.substringBefore(':', "").toIntOrNull() ?: return@mapNotNull null
                val pkg = entry.substringAfter(':', "").trim()
                if (slot < 0 || slot >= HOME_PER_PAGE * MAX_HOME_PAGES || pkg.isEmpty()) null else HomeShortcut(slot, pkg)
            }
            .filter { seenSlots.add(it.slot) && seenPackages.add(it.packageName) }
    }

    private fun loadWidgetIds(): List<Int> =
        sp.getString(K_WIDGET_IDS, "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .distinct()

    private fun loadDock(): List<String>? {
        val raw = sp.getString(K_DOCK, null) ?: return null
        return raw.split("|").filter { it.isNotBlank() }.distinct()
    }

    private fun loadPackageSet(key: String): Set<String> =
        sp.getString(key, "")
            .orEmpty()
            .split("|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    companion object {
        const val SORT_NAME_ASC = "name_asc"
        const val SORT_NAME_DESC = "name_desc"
        const val SORT_FAVORITES = "favorites"
        val SORT_OPTIONS = setOf(SORT_NAME_ASC, SORT_NAME_DESC, SORT_FAVORITES)

        private const val K_WIDGETS = "widgets_enabled"
        private const val K_OTG = "otg_enabled"
        private const val K_KEYS = "keyboard_shortcuts"
        private const val K_GLASS = "glass_opacity"
        private const val K_WIDGET_IDS = "widget_ids"
        private const val K_DOCK = "dock_packages"
        private const val K_HOME = "home_shortcuts"
        private const val K_FAVORITES = "favorite_packages"
        private const val K_HIDDEN = "hidden_packages"
        private const val K_APP_SORT = "app_sort"
    }
}
