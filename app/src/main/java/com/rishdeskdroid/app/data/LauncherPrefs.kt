package com.rishdeskdroid.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/** How the app drawer is ordered. */
enum class SortMode(val label: String) {
    AZ("A to Z"),
    ZA("Z to A"),
    NEWEST("Recently installed"),
    RECENT("Recently used");

    companion object {
        fun from(name: String?): SortMode = entries.firstOrNull { it.name == name } ?: AZ
    }
}

/** How the launcher wallpaper photo is fitted to the screen. */
enum class WallpaperFit(val label: String) {
    FILL("Fill"),
    FIT("Fit"),
    STRETCH("Stretch");

    companion object {
        fun from(name: String?): WallpaperFit = entries.firstOrNull { it.name == name } ?: FILL
    }
}

/** Where the dock sits along the bottom edge. */
enum class DockAlign(val label: String) {
    START("Left"),
    CENTER("Center"),
    END("Right");

    companion object {
        fun from(name: String?): DockAlign = entries.firstOrNull { it.name == name } ?: CENTER
    }
}

/** Size of a home-screen widget in widget cells (1 cell = [WIDGET_UNIT_DP] dp). */
data class WidgetSize(val w: Int, val h: Int)

/** Accent colours offered in Settings (ARGB). The first one is the default. */
val ACCENT_COLORS = listOf(
    0xFF3B82F6.toInt(), // blue
    0xFF8B5CF6.toInt(), // violet
    0xFFEC4899.toInt(), // pink
    0xFF10B981.toInt(), // green
    0xFFF59E0B.toInt(), // amber
    0xFF14B8A6.toInt(), // teal
)

/**
 * All launcher settings, backed by SharedPreferences and exposed as Compose state so the UI
 * updates the moment a value changes. Everything is stored on the device only.
 */
class LauncherPrefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("glass_launcher", Context.MODE_PRIVATE)

    // ---- Existing settings (keys unchanged so upgrades keep their values) -----------------------

    /** Settings > Widgets ON/OFF */
    var widgetsEnabled by mutableStateOf(sp.getBoolean(K_WIDGETS, true))
        private set

    /** Settings > OTG Shortcut ON/OFF */
    var otgEnabled by mutableStateOf(sp.getBoolean(K_OTG, true))
        private set

    /** Settings > Keyboard shortcuts ON/OFF */
    var keyboardEnabled by mutableStateOf(sp.getBoolean(K_KEYS, true))
        private set

    /** Settings > Glass Opacity, 0f..1f */
    var glassOpacity by mutableFloatStateOf(sp.getFloat(K_GLASS, DEFAULT_GLASS))
        private set

    /** Bumped whenever the wallpaper file changes so the UI reloads it. */
    var wallpaperVersion by mutableIntStateOf(0)
        private set

    /** AppWidget ids placed on the home screen, in display order. */
    var widgetIds by mutableStateOf(loadWidgetIds())
        private set

    /** Package names pinned in the dock, or null to use the default dock. */
    var dockPackages by mutableStateOf<List<String>?>(loadDock())
        private set

    // ---- Home screen ----------------------------------------------------------------------------

    /** Shortcuts and folders on the home screen. */
    var homeItems by mutableStateOf(loadHomeItems())
        private set

    var desktopColumns by mutableIntStateOf(sp.getInt(K_D_COLS, DEFAULT_D_COLS).coerceIn(3, 7))
        private set
    var desktopRows by mutableIntStateOf(sp.getInt(K_D_ROWS, DEFAULT_D_ROWS).coerceIn(2, 6))
        private set
    var desktopIconScale by mutableFloatStateOf(sp.getFloat(K_D_ICON, 1f).coerceIn(0.7f, 1.3f))
        private set
    var desktopSpacing by mutableIntStateOf(sp.getInt(K_D_SPACE, DEFAULT_SPACING).coerceIn(0, 24))
        private set
    var showDesktopLabels by mutableStateOf(sp.getBoolean(K_D_LABELS, true))
        private set

    /** Custom names for home-screen shortcuts, by package name. */
    var customLabels by mutableStateOf(loadLabels())
        private set

    /** Widget sizes by widget id. */
    var widgetSizes by mutableStateOf(loadWidgetSizes())
        private set

    // ---- Drawer ---------------------------------------------------------------------------------

    var sortMode by mutableStateOf(SortMode.from(sp.getString(K_SORT, null)))
        private set
    var favorites by mutableStateOf(loadList(K_FAVORITES))
        private set
    var hiddenApps by mutableStateOf(loadList(K_HIDDEN))
        private set
    var drawerColumns by mutableIntStateOf(sp.getInt(K_W_COLS, DEFAULT_W_COLS).coerceIn(3, 8))
        private set
    var drawerRows by mutableIntStateOf(sp.getInt(K_W_ROWS, DEFAULT_W_ROWS).coerceIn(2, 5))
        private set
    var drawerIconScale by mutableFloatStateOf(sp.getFloat(K_W_ICON, 1f).coerceIn(0.7f, 1.3f))
        private set
    var showDrawerLabels by mutableStateOf(sp.getBoolean(K_W_LABELS, true))
        private set

    // ---- Dock and taskbar -----------------------------------------------------------------------

    var showRecentsButton by mutableStateOf(sp.getBoolean(K_RECENTS_BTN, true))
        private set
    var dockAlign by mutableStateOf(DockAlign.from(sp.getString(K_DOCK_ALIGN, null)))
        private set
    var dockScale by mutableFloatStateOf(sp.getFloat(K_DOCK_SCALE, 1f).coerceIn(0.8f, 1.3f))
        private set

    // ---- Appearance and system ------------------------------------------------------------------

    var accent by mutableIntStateOf(sp.getInt(K_ACCENT, ACCENT_COLORS[0]))
        private set
    var animations by mutableStateOf(sp.getBoolean(K_ANIM, true))
        private set
    var showDate by mutableStateOf(sp.getBoolean(K_DATE, false))

    /** The welcome card with the Rish Innovations logo, shown once each time the launcher starts. */
    var showGreeting by mutableStateOf(sp.getBoolean(K_GREETING, true))
        private set
    var showSystemStatusBar by mutableStateOf(sp.getBoolean(K_SYS_BAR, false))
        private set
    var landscapeApps by mutableStateOf(sp.getBoolean(K_LANDSCAPE_APPS, true))
        private set
    var windowedApps by mutableStateOf(sp.getBoolean(K_WINDOWED, false))
        private set

    // ---- Wallpaper ------------------------------------------------------------------------------

    var wallpaperFit by mutableStateOf(WallpaperFit.from(sp.getString(K_WP_FIT, null)))
        private set
    var wallpaperX by mutableFloatStateOf(sp.getFloat(K_WP_X, 0.5f).coerceIn(0f, 1f))
        private set
    var wallpaperY by mutableFloatStateOf(sp.getFloat(K_WP_Y, 0.5f).coerceIn(0f, 1f))
        private set
    var wallpaperDim by mutableFloatStateOf(sp.getFloat(K_WP_DIM, 0f).coerceIn(0f, 0.8f))
        private set
    var wallpaperBlur by mutableIntStateOf(sp.getInt(K_WP_BLUR, 0).coerceIn(0, 25))
        private set

    // ---- Updates --------------------------------------------------------------------------------

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
        val v = value.coerceIn(0f, 1f)
        glassOpacity = v
        sp.edit().putFloat(K_GLASS, v).apply()
    }

    fun bumpWallpaper() {
        wallpaperVersion += 1
    }

    fun updateWidgetIds(ids: List<Int>) {
        widgetIds = ids.distinct()
        sp.edit().putString(K_WIDGET_IDS, widgetIds.joinToString(",")).apply()
        // Forget sizes of widgets that no longer exist
        val kept = widgetSizes.filterKeys { it in widgetIds }
        if (kept.size != widgetSizes.size) updateWidgetSizes(kept)
    }

    fun updateDockPackages(packages: List<String>) {
        dockPackages = packages.distinct()
        sp.edit().putString(K_DOCK, dockPackages!!.joinToString("|")).apply()
    }

    fun resetDock() {
        dockPackages = null
        sp.edit().remove(K_DOCK).apply()
    }

    fun updateHomeItems(items: List<HomeItem>) {
        homeItems = items
        sp.edit().putString(K_HOME2, HomeLayout.toJson(items)).apply()
    }

    fun updateDesktopGrid(columns: Int, rows: Int) {
        desktopColumns = columns.coerceIn(3, 7)
        desktopRows = rows.coerceIn(2, 6)
        sp.edit().putInt(K_D_COLS, desktopColumns).putInt(K_D_ROWS, desktopRows).apply()
    }

    fun updateDesktopIconScale(v: Float) {
        desktopIconScale = v.coerceIn(0.7f, 1.3f)
        sp.edit().putFloat(K_D_ICON, desktopIconScale).apply()
    }

    fun updateDesktopSpacing(v: Int) {
        desktopSpacing = v.coerceIn(0, 24)
        sp.edit().putInt(K_D_SPACE, desktopSpacing).apply()
    }

    fun updateShowDesktopLabels(v: Boolean) {
        showDesktopLabels = v
        sp.edit().putBoolean(K_D_LABELS, v).apply()
    }

    fun updateCustomLabel(packageName: String, label: String?) {
        val map = customLabels.toMutableMap()
        val clean = label?.trim().orEmpty()
        if (clean.isEmpty()) map.remove(packageName) else map[packageName] = clean.take(40)
        customLabels = map
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        sp.edit().putString(K_LABELS, json.toString()).apply()
    }

    fun updateWidgetSizes(sizes: Map<Int, WidgetSize>) {
        widgetSizes = sizes
        val json = JSONObject()
        sizes.forEach { (id, s) -> json.put(id.toString(), "${s.w}x${s.h}") }
        sp.edit().putString(K_WIDGET_SIZES, json.toString()).apply()
    }

    fun updateSortMode(mode: SortMode) {
        sortMode = mode
        sp.edit().putString(K_SORT, mode.name).apply()
    }

    fun toggleFavorite(packageName: String) {
        updateFavorites(if (packageName in favorites) favorites - packageName else favorites + packageName)
    }

    private fun updateFavorites(list: List<String>) {
        favorites = list
        saveList(K_FAVORITES, list)
    }

    fun updateHidden(packageName: String, hidden: Boolean) {
        hiddenApps = if (hidden) (hiddenApps + packageName).distinct() else hiddenApps - packageName
        saveList(K_HIDDEN, hiddenApps)
    }

    fun updateDrawerGrid(columns: Int, rows: Int) {
        drawerColumns = columns.coerceIn(3, 8)
        drawerRows = rows.coerceIn(2, 5)
        sp.edit().putInt(K_W_COLS, drawerColumns).putInt(K_W_ROWS, drawerRows).apply()
    }

    fun updateDrawerIconScale(v: Float) {
        drawerIconScale = v.coerceIn(0.7f, 1.3f)
        sp.edit().putFloat(K_W_ICON, drawerIconScale).apply()
    }

    fun updateShowDrawerLabels(v: Boolean) {
        showDrawerLabels = v
        sp.edit().putBoolean(K_W_LABELS, v).apply()
    }

    fun updateShowRecentsButton(v: Boolean) {
        showRecentsButton = v
        sp.edit().putBoolean(K_RECENTS_BTN, v).apply()
    }

    fun updateDockAlign(v: DockAlign) {
        dockAlign = v
        sp.edit().putString(K_DOCK_ALIGN, v.name).apply()
    }

    fun updateDockScale(v: Float) {
        dockScale = v.coerceIn(0.8f, 1.3f)
        sp.edit().putFloat(K_DOCK_SCALE, dockScale).apply()
    }

    fun updateAccent(argb: Int) {
        accent = argb
        sp.edit().putInt(K_ACCENT, argb).apply()
    }

    fun updateAnimations(v: Boolean) {
        animations = v
        sp.edit().putBoolean(K_ANIM, v).apply()
    }

    fun updateShowGreeting(v: Boolean) {
        showGreeting = v
        sp.edit().putBoolean(K_GREETING, v).apply()
    }

    fun updateShowDate(v: Boolean) {
        showDate = v
        sp.edit().putBoolean(K_DATE, v).apply()
    }

    fun updateShowSystemStatusBar(v: Boolean) {
        showSystemStatusBar = v
        sp.edit().putBoolean(K_SYS_BAR, v).apply()
    }

    fun updateLandscapeApps(v: Boolean) {
        landscapeApps = v
        sp.edit().putBoolean(K_LANDSCAPE_APPS, v).apply()
    }

    fun updateWindowedApps(v: Boolean) {
        windowedApps = v
        sp.edit().putBoolean(K_WINDOWED, v).apply()
    }

    fun updateWallpaperFit(v: WallpaperFit) {
        wallpaperFit = v
        sp.edit().putString(K_WP_FIT, v.name).apply()
    }

    fun updateWallpaperPosition(x: Float, y: Float) {
        wallpaperX = x.coerceIn(0f, 1f)
        wallpaperY = y.coerceIn(0f, 1f)
        sp.edit().putFloat(K_WP_X, wallpaperX).putFloat(K_WP_Y, wallpaperY).apply()
    }

    fun updateWallpaperDim(v: Float) {
        wallpaperDim = v.coerceIn(0f, 0.8f)
        sp.edit().putFloat(K_WP_DIM, wallpaperDim).apply()
    }

    fun updateWallpaperBlur(v: Int) {
        wallpaperBlur = v.coerceIn(0, 25)
        sp.edit().putInt(K_WP_BLUR, wallpaperBlur).apply()
    }

    // ---- Resets ---------------------------------------------------------------------------------

    /** Themes, transparency, animations and wallpaper adjustments. Keeps the wallpaper photo. */
    fun resetAppearance() {
        updateGlassOpacity(DEFAULT_GLASS)
        updateAccent(ACCENT_COLORS[0])
        updateAnimations(true)
        updateShowDate(false)
        updateShowGreeting(true)
        updateWallpaperFit(WallpaperFit.FILL)
        updateWallpaperPosition(0.5f, 0.5f)
        updateWallpaperDim(0f)
        updateWallpaperBlur(0)
    }

    /** Home shortcuts, folders, widget sizes, grid and icon settings. Keeps the dock and widgets. */
    fun resetDesktopLayout() {
        updateHomeItems(emptyList())
        updateDesktopGrid(DEFAULT_D_COLS, DEFAULT_D_ROWS)
        updateDesktopIconScale(1f)
        updateDesktopSpacing(DEFAULT_SPACING)
        updateShowDesktopLabels(true)
        updateWidgetSizes(emptyMap())
        updateCustomLabelsClear()
    }

    private fun updateCustomLabelsClear() {
        customLabels = emptyMap()
        sp.edit().remove(K_LABELS).apply()
    }

    /** Every launcher preference. Wallpaper photo, installed apps and widgets are not touched. */
    fun resetAll() {
        resetAppearance()
        resetDesktopLayout()
        resetDock()
        updateWidgetsEnabled(true)
        updateOtgEnabled(true)
        updateKeyboardEnabled(true)
        updateSortMode(SortMode.AZ)
        sp.edit().remove(K_FAVORITES).remove(K_HIDDEN).apply()
        favorites = emptyList()
        hiddenApps = emptyList()
        updateDrawerGrid(DEFAULT_W_COLS, DEFAULT_W_ROWS)
        updateDrawerIconScale(1f)
        updateShowDrawerLabels(true)
        updateShowRecentsButton(true)
        updateDockAlign(DockAlign.CENTER)
        updateDockScale(1f)
        updateShowSystemStatusBar(false)
        updateLandscapeApps(true)
        updateWindowedApps(false)
    }

    // ---- Loading --------------------------------------------------------------------------------

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

    private fun loadList(key: String): List<String> =
        sp.getString(key, "").orEmpty().split("|").filter { it.isNotBlank() }.distinct()

    private fun saveList(key: String, list: List<String>) {
        sp.edit().putString(key, list.joinToString("|")).apply()
    }

    private fun loadHomeItems(): List<HomeItem> {
        val v2 = sp.getString(K_HOME2, null)
        if (v2 != null) return HomeLayout.fromJson(v2)
        // Older builds stored "slot:package|slot:package"
        return HomeLayout.fromLegacy(sp.getString(K_HOME, ""))
    }

    private fun loadLabels(): Map<String, String> = try {
        val json = JSONObject(sp.getString(K_LABELS, "{}") ?: "{}")
        json.keys().asSequence().associateWith { json.getString(it) }
    } catch (e: Exception) {
        emptyMap()
    }

    private fun loadWidgetSizes(): Map<Int, WidgetSize> = try {
        val json = JSONObject(sp.getString(K_WIDGET_SIZES, "{}") ?: "{}")
        val result = HashMap<Int, WidgetSize>()
        json.keys().forEach { key ->
            val id = key.toIntOrNull() ?: return@forEach
            val parts = json.getString(key).split("x")
            val w = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
            val h = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
            result[id] = WidgetSize(w.coerceIn(1, 8), h.coerceIn(1, 4))
        }
        result
    } catch (e: Exception) {
        emptyMap()
    }

    private companion object {
        const val DEFAULT_GLASS = 0.6f
        const val DEFAULT_D_COLS = 5
        const val DEFAULT_D_ROWS = 4
        const val DEFAULT_W_COLS = 5
        const val DEFAULT_W_ROWS = 3
        const val DEFAULT_SPACING = 4

        const val K_WIDGETS = "widgets_enabled"
        const val K_OTG = "otg_enabled"
        const val K_KEYS = "keyboard_shortcuts"
        const val K_GLASS = "glass_opacity"
        const val K_WIDGET_IDS = "widget_ids"
        const val K_DOCK = "dock_packages"
        const val K_HOME = "home_shortcuts"
        const val K_HOME2 = "home_items_v2"
        const val K_D_COLS = "desktop_columns"
        const val K_D_ROWS = "desktop_rows"
        const val K_D_ICON = "desktop_icon_scale"
        const val K_D_SPACE = "desktop_spacing"
        const val K_D_LABELS = "desktop_labels"
        const val K_LABELS = "custom_labels"
        const val K_WIDGET_SIZES = "widget_sizes"
        const val K_SORT = "sort_mode"
        const val K_FAVORITES = "favorite_apps"
        const val K_HIDDEN = "hidden_apps"
        const val K_W_COLS = "drawer_columns"
        const val K_W_ROWS = "drawer_rows"
        const val K_W_ICON = "drawer_icon_scale"
        const val K_W_LABELS = "drawer_labels"
        const val K_RECENTS_BTN = "recents_button"
        const val K_DOCK_ALIGN = "dock_align"
        const val K_DOCK_SCALE = "dock_scale"
        const val K_ACCENT = "accent_color"
        const val K_ANIM = "animations"
        const val K_DATE = "show_date"
        const val K_GREETING = "show_greeting"
        const val K_SYS_BAR = "show_system_status_bar"
        const val K_LANDSCAPE_APPS = "landscape_apps"
        const val K_WINDOWED = "windowed_apps"
        const val K_WP_FIT = "wallpaper_fit"
        const val K_WP_X = "wallpaper_x"
        const val K_WP_Y = "wallpaper_y"
        const val K_WP_DIM = "wallpaper_dim"
        const val K_WP_BLUR = "wallpaper_blur"
    }
}
