package com.rishdeskdroid.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsFilterTest {

    private val apps = listOf(
        AppInfo("YouTube", "com.google.youtube", "Main"),
        AppInfo("Browser", "com.example.browser", "Main"),
        AppInfo("Camera", "com.example.camera", "Main"),
        AppInfo("YouTube Music", "com.google.music", "Main"),
    )

    private val homeShortcuts = listOf(
        HomeShortcut(0, "a"),
        HomeShortcut(1, "b"),
        HomeShortcut(5, "c"),
    )

    @Test
    fun homeShortcutMoveSwapsWithOccupiedAdjacentCell() {
        assertEquals(
            listOf(HomeShortcut(0, "b"), HomeShortcut(1, "a"), HomeShortcut(5, "c")),
            reorderHomeShortcuts(homeShortcuts, "a", 1),
        )
    }

    @Test
    fun homeShortcutMoveUsesEmptyCellWithoutChangingOtherShortcuts() {
        assertEquals(
            listOf(HomeShortcut(0, "a"), HomeShortcut(1, "b"), HomeShortcut(6, "c")),
            reorderHomeShortcuts(homeShortcuts, "c", 1),
        )
    }

    @Test
    fun homeShortcutMovesUpAndDownWithinPage() {
        val shortcuts = listOf(HomeShortcut(5, "a"), HomeShortcut(10, "b"))
        assertEquals(
            listOf(HomeShortcut(0, "a"), HomeShortcut(10, "b")),
            reorderHomeShortcuts(shortcuts, "a", -HOME_COLUMNS),
        )
        assertEquals(
            listOf(HomeShortcut(5, "b"), HomeShortcut(10, "a")),
            reorderHomeShortcuts(shortcuts, "a", HOME_COLUMNS),
        )
    }

    @Test
    fun homeShortcutMoveRespectsGridEdgesAndInvalidRequests() {
        val rowStart = listOf(HomeShortcut(5, "a"))
        val rowEnd = listOf(HomeShortcut(9, "a"))
        val topRow = listOf(HomeShortcut(1, "a"))
        val bottomRow = listOf(HomeShortcut(16, "a"))
        assertEquals(rowStart, reorderHomeShortcuts(rowStart, "a", -1))
        assertEquals(rowEnd, reorderHomeShortcuts(rowEnd, "a", 1))
        assertEquals(topRow, reorderHomeShortcuts(topRow, "a", -HOME_COLUMNS))
        assertEquals(bottomRow, reorderHomeShortcuts(bottomRow, "a", HOME_COLUMNS))
        assertEquals(homeShortcuts, reorderHomeShortcuts(homeShortcuts, "missing", 1))
        assertEquals(homeShortcuts, reorderHomeShortcuts(homeShortcuts, "a", 2))
    }

    @Test
    fun searchMatchesLabelsCaseInsensitively() {
        val result = filterAndSortApps(
            apps = apps,
            query = "yOuTuBe",
            hiddenPackages = emptySet(),
            showHidden = false,
            favoritePackages = emptySet(),
            sort = LauncherPrefs.SORT_NAME_ASC,
        )
        assertEquals(listOf("YouTube", "YouTube Music"), result.map { it.label })
    }

    @Test
    fun searchCanMatchPackageNames() {
        val result = filterAndSortApps(
            apps = apps,
            query = "com.example",
            hiddenPackages = emptySet(),
            showHidden = false,
            favoritePackages = emptySet(),
            sort = LauncherPrefs.SORT_NAME_ASC,
        )
        assertEquals(listOf("Browser", "Camera"), result.map { it.label })
    }

    @Test
    fun hiddenAppsAreExcludedUntilHiddenViewIsSelected() {
        val hidden = setOf("com.example.camera")
        val normal = filterAndSortApps(apps, "", hidden, false, emptySet(), LauncherPrefs.SORT_NAME_ASC)
        val hiddenView = filterAndSortApps(apps, "", hidden, true, emptySet(), LauncherPrefs.SORT_NAME_ASC)
        assertEquals(listOf("Browser", "YouTube", "YouTube Music"), normal.map { it.label })
        assertEquals(listOf("Camera"), hiddenView.map { it.label })
    }

    @Test
    fun favoritesSortPlacesFavoritesFirstAndKeepsAlphabeticalOrder() {
        val result = filterAndSortApps(
            apps = apps,
            query = "",
            hiddenPackages = emptySet(),
            showHidden = false,
            favoritePackages = setOf("com.google.music", "com.example.browser"),
            sort = LauncherPrefs.SORT_FAVORITES,
        )
        assertEquals(listOf("Browser", "YouTube Music", "Camera", "YouTube"), result.map { it.label })
    }

    @Test
    fun descendingSortUsesReverseAlphabeticalOrder() {
        val result = filterAndSortApps(apps, "", emptySet(), false, emptySet(), LauncherPrefs.SORT_NAME_DESC)
        assertEquals(listOf("YouTube Music", "YouTube", "Camera", "Browser"), result.map { it.label })
    }
}
