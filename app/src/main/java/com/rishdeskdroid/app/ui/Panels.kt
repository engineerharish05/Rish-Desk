package com.rishdeskdroid.app.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.HomeFolder
import com.rishdeskdroid.app.data.HomeLayout
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.SpanLimits
import com.rishdeskdroid.app.data.WidgetSize
import com.rishdeskdroid.app.data.hiddenAppList
import com.rishdeskdroid.app.data.homePerPage
import com.rishdeskdroid.app.data.launchApp

private val PanelSurface = Color(0xF2171A3F)

/** The frame used by every panel: dark glass, rounded, sized for landscape. */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.6f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier
                .fillMaxWidth(widthFraction)
                .heightIn(max = maxHeight)
                .background(PanelSurface, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun PanelTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** A rounded button used in panels and in Settings. */
@Composable
fun GlassButton(
    text: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = LocalAccent.current
    val fill = if (primary) accent else Color.White.copy(alpha = 0.2f)
    Box(
        modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (enabled) fill else fill.copy(alpha = fill.alpha * 0.4f))
            .border(1.dp, Color.White.copy(alpha = if (primary) 0f else 0.4f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/** A glass text field (search box, rename box). */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    onAction: () -> Unit = {},
    trailing: (@Composable () -> Unit)? = null,
) {
    val requesterModifier = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = imeAction, capitalization = capitalization),
        keyboardActions = KeyboardActions(onAny = { onAction() }),
        modifier = modifier.then(requesterModifier),
        decorationBox = { inner ->
            val shape = RoundedCornerShape(22.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .background(Color.White.copy(alpha = 0.16f), shape)
                    .border(1.dp, Color.White.copy(alpha = 0.34f), shape)
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = Color.White.copy(alpha = 0.55f), fontSize = 15.sp, maxLines = 1)
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/** A small round-rectangle step button ("-" / "+"). */
@Composable
fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(44.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (enabled) 0.2f else 0.08f))
            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.4f else 0.15f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** "-  3  +" with a range. */
@Composable
fun Stepper(
    value: Int,
    min: Int,
    max: Int,
    description: String,
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton("−", "Decrease $description", value > min) { onChange((value - 1).coerceAtLeast(min)) }
        Text(
            value.toString(),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        StepButton("+", "Increase $description", value < max) { onChange((value + 1).coerceAtMost(max)) }
    }
}

/** A row of mutually exclusive options. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) accent else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 14.dp)
                    .semantics { contentDescription = label(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    GlassDialog(onDismiss, widthFraction = 0.5f) {
        PanelTitle(title)
        Text(message, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
            GlassButton("Cancel", onClick = onDismiss)
            GlassButton(confirmLabel, primary = true, onClick = onConfirm)
        }
    }
}

@Composable
fun RenameDialog(
    title: String,
    initial: String,
    hint: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    GlassDialog(onDismiss, widthFraction = 0.5f) {
        PanelTitle(title)
        GlassTextField(
            value = text,
            onValueChange = { text = it.take(40) },
            placeholder = hint,
            capitalization = KeyboardCapitalization.Words,
            onAction = { onSave(text) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
            GlassButton("Cancel", onClick = onDismiss)
            GlassButton("Save", primary = true, onClick = { onSave(text) })
        }
    }
}

/** Hidden apps with a Restore button each. */
@Composable
fun HiddenAppsDialog(prefs: LauncherPrefs, apps: List<AppInfo>, onDismiss: () -> Unit) {
    val hidden = hiddenAppList(prefs, apps)
    GlassDialog(onDismiss) {
        PanelTitle("Hidden apps")
        Text(
            "Hidden apps are still installed. Restore one to show it in the app drawer again.",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp,
        )
        if (hidden.isEmpty()) {
            Text("No hidden apps.", color = Color.White, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp))
        } else {
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(hidden, key = { it.packageName }) { app ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppIconImage(app, 40.dp)
                        Text(
                            app.label,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        GlassButton("Restore") { prefs.updateHidden(app.packageName, false) }
                    }
                }
            }
        }
        GlassButton("Done", primary = true, modifier = Modifier.align(Alignment.End), onClick = onDismiss)
    }
}

/** Resize a widget within the limits its provider allows. */
@Composable
fun ResizeWidgetDialog(
    label: String,
    limits: SpanLimits,
    size: WidgetSize,
    onChange: (WidgetSize) -> Unit,
    onDismiss: () -> Unit,
) {
    GlassDialog(onDismiss, widthFraction = 0.5f) {
        PanelTitle("Resize $label")
        if (!limits.resizable) {
            Text(
                "This widget has a fixed size. Its app does not allow resizing.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Width", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Stepper(size.w, limits.minW, limits.maxW, "width") { onChange(WidgetSize(it, size.h)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Height", color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Stepper(size.h, limits.minH, limits.maxH, "height") { onChange(WidgetSize(size.w, it)) }
            }
        }
        GlassButton("Done", primary = true, modifier = Modifier.align(Alignment.End), onClick = onDismiss)
    }
}

/** An open folder: launch, rename, add and remove apps, or delete the folder (its apps return to Home). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderDialog(
    prefs: LauncherPrefs,
    folderId: String,
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val folder: HomeFolder? = HomeLayout.folder(prefs.homeItems, folderId)
    if (folder == null) {
        // The folder was dissolved (for example its apps were uninstalled): close the panel
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var name by remember(folderId) { mutableStateOf(folder.name) }
    var showPicker by remember { mutableStateOf(false) }
    val limit = HomeLayout.maxSlots(homePerPage(prefs))
    val installed = remember(apps) { apps.distinctBy { it.packageName }.associateBy { it.packageName } }
    val members = folder.members.mapNotNull { installed[it] }

    fun saveName() {
        prefs.updateHomeItems(HomeLayout.renameFolder(prefs.homeItems, folderId, name))
    }

    GlassDialog(onDismiss = {
        saveName()
        onDismiss()
    }) {
        GlassTextField(
            value = name,
            onValueChange = { name = it.take(30) },
            placeholder = "Folder name",
            capitalization = KeyboardCapitalization.Words,
            onAction = { saveName() },
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(92.dp),
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(members, key = { it.packageName }) { app ->
                var menu by remember { mutableStateOf(false) }
                val source = remember { MutableInteractionSource() }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier
                            .pressScale(source, 1.07f)
                            .combinedClickable(
                                interactionSource = source,
                                indication = null,
                                onClick = {
                                    saveName()
                                    onDismiss()
                                    launchApp(context, app)
                                },
                                onLongClick = { menu = true },
                            )
                            .semantics { contentDescription = app.label }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AppIconImage(app, 48.dp)
                        Text(
                            app.label,
                            color = Color.White,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = LabelStyle,
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove from folder") },
                            onClick = {
                                menu = false
                                val updated = HomeLayout.removeFromFolder(prefs.homeItems, folderId, app.packageName, limit)
                                if (updated == null) {
                                    Toast.makeText(context, "The home screen is full", Toast.LENGTH_SHORT).show()
                                } else {
                                    prefs.updateHomeItems(updated)
                                }
                            },
                        )
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.align(Alignment.End)) {
            GlassButton("Add app") { showPicker = true }
            GlassButton("Delete folder") {
                prefs.updateHomeItems(HomeLayout.deleteFolder(prefs.homeItems, folderId, limit))
                onDismiss()
            }
            GlassButton("Done", primary = true) {
                saveName()
                onDismiss()
            }
        }
    }

    if (showPicker) {
        val hidden = prefs.hiddenApps.toHashSet()
        val inFolders = HomeLayout.packagesOf(prefs.homeItems.filterIsInstance<HomeFolder>())
        AppPickerDialog(
            title = "Add to folder",
            apps = apps
                .distinctBy { it.packageName }
                .filter { it.packageName !in hidden && it.packageName !in inFolders },
            onPick = { app ->
                showPicker = false
                prefs.updateHomeItems(HomeLayout.addToFolder(prefs.homeItems, folderId, app.packageName))
            },
            onDismiss = { showPicker = false },
        )
    }
}
