package com.rishdeskdroid.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    private fun apps(vararg pairs: Pair<Int, String>): List<HomeItem> = pairs.map { HomeApp(it.first, it.second) }

    @Test
    fun addUsesFirstFreeCellAndRejectsDuplicates() {
        val start = apps(0 to "a", 2 to "b")
        val added = HomeLayout.add(start, "c", limit = 20)
        assertNotNull(added)
        assertEquals(1, (added!!.last() as HomeApp).slot)
        assertNull(HomeLayout.add(added, "c", limit = 20))
    }

    @Test
    fun addFailsWhenHomeIsFull() {
        val full = apps(0 to "a", 1 to "b")
        assertNull(HomeLayout.add(full, "c", limit = 2))
    }

    @Test
    fun dropOnEmptyCellMovesItem() {
        val moved = HomeLayout.drop(apps(0 to "a"), fromSlot = 0, toSlot = 7)
        assertEquals(listOf(HomeApp(7, "a")), moved)
    }

    @Test
    fun dropAppOnAppMakesFolderWithBothApps() {
        val result = HomeLayout.drop(apps(0 to "a", 1 to "b"), fromSlot = 0, toSlot = 1)
        assertEquals(1, result.size)
        val folder = result[0] as HomeFolder
        assertEquals(1, folder.slot)
        assertEquals(setOf("a", "b"), folder.members.toSet())
    }

    @Test
    fun dropAppOnFolderJoinsIt() {
        val folder = HomeFolder(1, "f", "Folder", listOf("b", "c"))
        val result = HomeLayout.drop(listOf(HomeApp(0, "a"), folder), fromSlot = 0, toSlot = 1)
        assertEquals(1, result.size)
        assertEquals(listOf("b", "c", "a"), (result[0] as HomeFolder).members)
    }

    @Test
    fun folderDroppedOnAnythingIsIgnored() {
        val items = listOf(HomeFolder(0, "f", "Folder", listOf("a", "b")), HomeApp(1, "c"))
        assertEquals(items, HomeLayout.drop(items, fromSlot = 0, toSlot = 1))
    }

    @Test
    fun removingFromFolderDownToOneTurnsItIntoAShortcut() {
        val items = listOf(HomeFolder(3, "f", "Folder", listOf("a", "b")))
        val result = HomeLayout.remove(items, "a")
        assertEquals(listOf(HomeApp(3, "b")), result)
    }

    @Test
    fun removeDeletesEmptyFolder() {
        val items = listOf(HomeFolder(3, "f", "Folder", listOf("a")))
        assertTrue(HomeLayout.remove(items, "a").isEmpty())
    }

    @Test
    fun removeFromFolderPutsAppBackOnHome() {
        val items = listOf(HomeFolder(0, "f", "Folder", listOf("a", "b", "c")))
        val result = HomeLayout.removeFromFolder(items, "f", "b", limit = 10)!!
        assertEquals(setOf("a", "b", "c"), HomeLayout.packagesOf(result))
        assertEquals(2, result.size)
        assertEquals(result.map { it.slot }.toSet().size, result.size)
    }

    @Test
    fun deleteFolderKeepsEveryApp() {
        val items = listOf(HomeFolder(0, "f", "Folder", listOf("a", "b", "c")))
        val result = HomeLayout.deleteFolder(items, "f", limit = 10)
        assertEquals(setOf("a", "b", "c"), HomeLayout.packagesOf(result))
        assertEquals(3, result.map { it.slot }.toSet().size)
    }

    @Test
    fun pruneForgetsUninstalledApps() {
        val items = listOf(
            HomeApp(0, "gone"),
            HomeApp(1, "kept"),
            HomeFolder(2, "f", "Folder", listOf("kept2", "gone2")),
        )
        val result = HomeLayout.prune(items) { it.startsWith("kept") }
        assertEquals(listOf(HomeApp(1, "kept"), HomeApp(2, "kept2")), result)
    }

    @Test
    fun renameFallsBackToDefaultName() {
        val items = listOf(HomeFolder(0, "f", "Work", listOf("a", "b")))
        val renamed = HomeLayout.renameFolder(items, "f", "   ")
        assertEquals(HomeLayout.DEFAULT_FOLDER_NAME, (renamed[0] as HomeFolder).name)
    }
}
