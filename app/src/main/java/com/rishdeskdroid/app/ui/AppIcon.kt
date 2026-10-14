package com.rishdeskdroid.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rishdeskdroid.app.data.AppInfo
import com.rishdeskdroid.app.data.IconLoader
import com.rishdeskdroid.app.data.LoadedIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAppIcon(app: AppInfo): LoadedIcon? {
    val context = LocalContext.current
    // Reading the version makes visible icons reload after IconLoader.invalidate
    val version = IconLoader.version
    val state = produceState<LoadedIcon?>(
        initialValue = IconLoader.peek(app),
        key1 = app.packageName,
        key2 = app.activityName,
        key3 = version,
    ) {
        // produceState keeps its previous value when the keys change, and grid cells are reused when
        // the list changes (for example switching to Favorites). Always reset to this app's own icon
        // so a reused cell can never keep showing another app's picture.
        val cached = IconLoader.peek(app)
        value = cached
        if (cached == null) {
            value = withContext(Dispatchers.Default) {
                IconLoader.load(context.packageManager, app)
            }
        }
    }
    return state.value
}

/** An app icon drawn as a macOS-style rounded square. */
@Composable
fun AppIconImage(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    val icon = rememberAppIcon(app)
    val shape = RoundedCornerShape(size * 0.23f)
    Box(modifier.size(size).shadow(5.dp, shape, clip = false)) {
        if (icon != null) {
            val clipped = if (icon.adaptive) Modifier.clip(shape) else Modifier
            Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().then(clipped),
            )
        } else {
            Box(Modifier.fillMaxSize().clip(shape).background(Color.White.copy(alpha = 0.15f)))
        }
    }
}
