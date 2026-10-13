package com.rishdeskdroid.app.data

import android.content.pm.PackageManager
import java.text.Collator

/** Which apps the drawer shows. */
enum class DrawerFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
}

/** The system Settings app is never hidden, so the user can always get back to settings. */
fun canHide(packageName: String): Boolean = packageName != "com.android.settings"

private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

fun sortApps(list: List<AppInfo>, mode: SortMode): List<AppInfo> {
    val byLabel = Comparator<AppInfo> { a, b -> collator.compare(a.label, b.label) }
    return when (mode) {
        SortMode.AZ -> list.sortedWith(byLabel)
        SortMode.ZA -> list.sortedWith(byLabel.reversed())
        SortMode.NEWEST -> list.sortedWith(compareByDescending<AppInfo> { it.installTime }.then(byLabel))
        SortMode.RECENT -> {
            val order = RecentStore.recents
            list.sortedWith(
                compareBy<AppInfo> {
                    val index = order.indexOf(it.packageName)
                    if (index < 0) Int.MAX_VALUE else index
                }.then(byLabel),
            )
        }
    }
}

/** The apps the drawer should show for the current filter, search text and sort order. */
fun drawerApps(prefs: LauncherPrefs, apps: List<AppInfo>, filter: DrawerFilter, query: String): List<AppInfo> {
    val hidden = prefs.hiddenApps.toHashSet()
    val visible = apps.filter { it.packageName !in hidden }
    val text = query.trim()
    val matching = if (text.isEmpty()) visible else visible.filter { it.label.contains(text, ignoreCase = true) }
    return when (filter) {
        DrawerFilter.ALL -> sortApps(matching, SortMode.AZ)
        DrawerFilter.FAVORITES -> {
            val favorites = prefs.favorites.toHashSet()
            sortApps(matching.filter { it.packageName in favorites }, SortMode.AZ)
        }
        DrawerFilter.RECENT -> RecentStore.recents.mapNotNull { pkg -> matching.firstOrNull { it.packageName == pkg } }
    }
}

/** Apps hidden from the drawer that are still installed, for the restore list. */
fun hiddenAppList(prefs: LauncherPrefs, apps: List<AppInfo>): List<AppInfo> {
    val hidden = prefs.hiddenApps.toHashSet()
    return sortApps(apps.filter { it.packageName in hidden }.distinctBy { it.packageName }, SortMode.AZ)
}

/** Recently opened apps (from this launcher) that are installed and not hidden. */
fun recentApps(prefs: LauncherPrefs, apps: List<AppInfo>): List<AppInfo> {
    val hidden = prefs.hiddenApps.toHashSet()
    return RecentStore.recents.mapNotNull { pkg ->
        apps.firstOrNull { it.packageName == pkg && it.packageName !in hidden }
    }
}

// ---- Home screen --------------------------------------------------------------------------------

enum class AddToHomeResult { Added, AlreadyOnHome, Unavailable, Full }

fun homePerPage(prefs: LauncherPrefs): Int = prefs.desktopColumns * prefs.desktopRows

/** The name shown under a home shortcut: the user's custom name if any, else the app's own. */
fun labelFor(prefs: LauncherPrefs, app: AppInfo): String = prefs.customLabels[app.packageName] ?: app.label

/** Puts the app in the first free home-screen cell (a new page opens when a page is full). */
fun addToHome(prefs: LauncherPrefs, apps: List<AppInfo>, app: AppInfo): AddToHomeResult {
    if (apps.none { it.packageName == app.packageName }) return AddToHomeResult.Unavailable
    val items = prefs.homeItems
    if (HomeLayout.contains(items, app.packageName)) return AddToHomeResult.AlreadyOnHome
    val updated = HomeLayout.add(items, app.packageName, HomeLayout.maxSlots(homePerPage(prefs)))
        ?: return AddToHomeResult.Full
    prefs.updateHomeItems(updated)
    return AddToHomeResult.Added
}

/** Removes only the home shortcut. The app stays installed and stays in the drawer. */
fun removeFromHome(prefs: LauncherPrefs, packageName: String) {
    prefs.updateHomeItems(HomeLayout.remove(prefs.homeItems, packageName))
}

/** Forgets shortcuts and folder entries whose app is no longer installed. */
fun pruneHome(prefs: LauncherPrefs, pm: PackageManager) {
    val pruned = HomeLayout.prune(prefs.homeItems) { pkg ->
        try {
            pm.getApplicationInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    if (pruned != prefs.homeItems) prefs.updateHomeItems(pruned)
}
