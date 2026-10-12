package com.rishdeskdroid.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Something that occupies one cell of the home screen. [slot] counts cells across all pages. */
sealed class HomeItem {
    abstract val slot: Int
}

data class HomeApp(override val slot: Int, val packageName: String) : HomeItem()

data class HomeFolder(
    override val slot: Int,
    val id: String,
    val name: String,
    val members: List<String>,
) : HomeItem()

/**
 * Pure functions that edit the home layout. Every function returns a new list and keeps these rules:
 *  - a package appears at most once on the whole home screen (as a shortcut or inside one folder);
 *  - no two items share a slot;
 *  - a folder always holds at least two apps (a folder that drops to one becomes a plain shortcut,
 *    and an empty one disappears), so no app is ever lost when a folder changes.
 */
object HomeLayout {

    const val MAX_PAGES = 10
    const val DEFAULT_FOLDER_NAME = "Folder"
    private const val MAX_SLOT = 2000

    fun maxSlots(perPage: Int): Int = perPage * MAX_PAGES

    fun packagesOf(items: List<HomeItem>): Set<String> {
        val result = HashSet<String>()
        for (item in items) {
            when (item) {
                is HomeApp -> result.add(item.packageName)
                is HomeFolder -> result.addAll(item.members)
            }
        }
        return result
    }

    fun contains(items: List<HomeItem>, packageName: String): Boolean = packageName in packagesOf(items)

    fun itemAt(items: List<HomeItem>, slot: Int): HomeItem? = items.firstOrNull { it.slot == slot }

    fun folder(items: List<HomeItem>, id: String): HomeFolder? =
        items.firstOrNull { it is HomeFolder && it.id == id } as HomeFolder?

    fun firstFree(items: List<HomeItem>, limit: Int): Int? {
        val used = items.map { it.slot }.toHashSet()
        return (0 until limit).firstOrNull { it !in used }
    }

    private fun withSlot(item: HomeItem, slot: Int): HomeItem = when (item) {
        is HomeApp -> item.copy(slot = slot)
        is HomeFolder -> item.copy(slot = slot)
    }

    /** Folders with fewer than two apps are turned back into shortcuts (or removed when empty). */
    fun normalize(items: List<HomeItem>): List<HomeItem> = items.mapNotNull { item ->
        if (item is HomeFolder) {
            when (item.members.size) {
                0 -> null
                1 -> HomeApp(item.slot, item.members[0])
                else -> item
            }
        } else {
            item
        }
    }

    /** Adds a shortcut in the first free cell. Returns null when the app is already there or the home screen is full. */
    fun add(items: List<HomeItem>, packageName: String, limit: Int): List<HomeItem>? {
        if (contains(items, packageName)) return null
        val slot = firstFree(items, limit) ?: return null
        return items + HomeApp(slot, packageName)
    }

    /** Removes the package everywhere on the home screen (the app itself stays installed). */
    fun remove(items: List<HomeItem>, packageName: String): List<HomeItem> = normalize(
        items.mapNotNull { item ->
            when (item) {
                is HomeApp -> if (item.packageName == packageName) null else item
                is HomeFolder -> item.copy(members = item.members - packageName)
            }
        },
    )

    /**
     * Drops the item in [fromSlot] onto [toSlot]. An empty cell moves it there; an app dropped on
     * an app makes a folder; an app dropped on a folder joins that folder. Anything else is ignored.
     */
    fun drop(
        items: List<HomeItem>,
        fromSlot: Int,
        toSlot: Int,
        folderName: String = DEFAULT_FOLDER_NAME,
    ): List<HomeItem> {
        if (fromSlot == toSlot || toSlot < 0 || toSlot >= MAX_SLOT) return items
        val source = itemAt(items, fromSlot) ?: return items
        val target = itemAt(items, toSlot)
        if (target == null) {
            return items.map { if (it === source) withSlot(it, toSlot) else it }
        }
        if (source !is HomeApp) return items
        return when (target) {
            is HomeApp -> {
                val folder = HomeFolder(
                    slot = toSlot,
                    id = UUID.randomUUID().toString().take(8),
                    name = folderName,
                    members = listOf(target.packageName, source.packageName),
                )
                items.filter { it !== source && it !== target } + folder
            }
            is HomeFolder -> {
                if (source.packageName in target.members) {
                    items
                } else {
                    items
                        .filter { it !== source }
                        .map { if (it === target) target.copy(members = target.members + source.packageName) else it }
                }
            }
        }
    }

    fun renameFolder(items: List<HomeItem>, id: String, name: String): List<HomeItem> {
        val clean = name.trim().take(30).ifEmpty { DEFAULT_FOLDER_NAME }
        return items.map { if (it is HomeFolder && it.id == id) it.copy(name = clean) else it }
    }

    /** Takes one app out of a folder and puts it back on the home screen. Null when there is no free cell. */
    fun removeFromFolder(items: List<HomeItem>, id: String, packageName: String, limit: Int): List<HomeItem>? {
        val folder = folder(items, id) ?: return items
        if (packageName !in folder.members) return items
        val slot = firstFree(items, limit) ?: return null
        val updated = items.map {
            if (it === folder) folder.copy(members = folder.members - packageName) else it
        } + HomeApp(slot, packageName)
        return normalize(updated)
    }

    /** Adds an app to a folder, moving it from the home screen if it was a shortcut there. */
    fun addToFolder(items: List<HomeItem>, id: String, packageName: String): List<HomeItem> {
        val folder = folder(items, id) ?: return items
        if (contains(items, packageName)) {
            // Only a loose shortcut may move into the folder
            val loose = items.firstOrNull { it is HomeApp && it.packageName == packageName } ?: return items
            return items
                .filter { it !== loose }
                .map { if (it === folder) folder.copy(members = folder.members + packageName) else it }
        }
        return items.map {
            if (it === folder) folder.copy(members = folder.members + packageName) else it
        }
    }

    /** Deletes a folder and puts every app in it back on the home screen, so nothing disappears. */
    fun deleteFolder(items: List<HomeItem>, id: String, limit: Int): List<HomeItem> {
        val folder = folder(items, id) ?: return items
        var result = items.filter { it !== folder }
        folder.members.forEachIndexed { index, pkg ->
            val slot = if (index == 0) folder.slot else firstFree(result, limit + folder.members.size)
            if (slot != null) result = result + HomeApp(slot, pkg)
        }
        return result
    }

    /** Drops every package for which [isInstalled] is false. */
    fun prune(items: List<HomeItem>, isInstalled: (String) -> Boolean): List<HomeItem> = normalize(
        items.mapNotNull { item ->
            when (item) {
                is HomeApp -> if (isInstalled(item.packageName)) item else null
                is HomeFolder -> item.copy(members = item.members.filter(isInstalled))
            }
        },
    )

    // ---- Storage --------------------------------------------------------------------------------

    fun toJson(items: List<HomeItem>): String {
        val array = JSONArray()
        for (item in items) {
            val o = JSONObject()
            when (item) {
                is HomeApp -> {
                    o.put("t", "a")
                    o.put("s", item.slot)
                    o.put("p", item.packageName)
                }
                is HomeFolder -> {
                    o.put("t", "f")
                    o.put("s", item.slot)
                    o.put("id", item.id)
                    o.put("n", item.name)
                    val members = JSONArray()
                    item.members.forEach { members.put(it) }
                    o.put("m", members)
                }
            }
            array.put(o)
        }
        return array.toString()
    }

    /** Reads saved items and repairs anything inconsistent (duplicates, bad slots, tiny folders). */
    fun fromJson(raw: String?): List<HomeItem> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            val parsed = ArrayList<HomeItem>()
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val slot = o.optInt("s", -1)
                if (slot < 0 || slot >= MAX_SLOT) continue
                when (o.optString("t")) {
                    "a" -> {
                        val pkg = o.optString("p")
                        if (pkg.isNotEmpty()) parsed.add(HomeApp(slot, pkg))
                    }
                    "f" -> {
                        val members = ArrayList<String>()
                        val m = o.optJSONArray("m")
                        if (m != null) {
                            for (j in 0 until m.length()) {
                                val p = m.optString(j)
                                if (p.isNotEmpty()) members.add(p)
                            }
                        }
                        val id = o.optString("id").ifEmpty { UUID.randomUUID().toString().take(8) }
                        parsed.add(HomeFolder(slot, id, o.optString("n").ifEmpty { DEFAULT_FOLDER_NAME }, members.distinct()))
                    }
                }
            }
            repair(parsed)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Older builds saved shortcuts as "slot:package|slot:package". */
    fun fromLegacy(raw: String?): List<HomeItem> {
        if (raw.isNullOrBlank()) return emptyList()
        val parsed = raw.split("|").mapNotNull { entry ->
            val slot = entry.substringBefore(':', "").toIntOrNull() ?: return@mapNotNull null
            val pkg = entry.substringAfter(':', "").trim()
            if (slot < 0 || slot >= MAX_SLOT || pkg.isEmpty()) null else HomeApp(slot, pkg)
        }
        return repair(parsed)
    }

    private fun repair(items: List<HomeItem>): List<HomeItem> {
        val slots = HashSet<Int>()
        val packages = HashSet<String>()
        val result = ArrayList<HomeItem>()
        for (item in items) {
            if (!slots.add(item.slot)) continue
            when (item) {
                is HomeApp -> if (packages.add(item.packageName)) result.add(item) else slots.remove(item.slot)
                is HomeFolder -> {
                    val members = item.members.filter { packages.add(it) }
                    result.add(item.copy(members = members))
                }
            }
        }
        return normalize(result)
    }
}
