package com.rishdeskdroid.app.ui

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rishdeskdroid.app.data.LauncherPrefs
import com.rishdeskdroid.app.data.WallpaperFit
import com.rishdeskdroid.app.data.WallpaperStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The launcher wallpaper: the user's photo, or the default wave design. */
@Composable
fun Wallpaper(prefs: LauncherPrefs, blurred: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = prefs.wallpaperVersion) {
        value = withContext(Dispatchers.IO) { WallpaperStore.load(context) }
    }
    val duration = if (prefs.animations) 300 else 0
    // The extra blur behind the drawer and settings, or the user's own blur, whichever is stronger
    val targetBlur = maxOf(if (blurred) 22 else 0, prefs.wallpaperBlur).dp
    val blurRadius by animateDpAsState(targetBlur, tween(duration), label = "wallpaperBlur")
    val zoom by animateFloatAsState(if (blurred) 1.06f else 1f, tween(duration), label = "wallpaperZoom")

    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                }
                .then(
                    if (Build.VERSION.SDK_INT >= 31 && blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier,
                ),
        ) {
            val photo = bitmap
            if (photo != null) {
                WallpaperImage(photo, prefs.wallpaperFit, prefs.wallpaperX, prefs.wallpaperY, Modifier.fillMaxSize())
            } else {
                DefaultWallpaper(Modifier.fillMaxSize())
            }
        }
        if (prefs.wallpaperDim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = prefs.wallpaperDim)))
        }
    }
}

/** A wallpaper photo drawn with the user's fit and position settings. */
@Composable
fun WallpaperImage(
    bitmap: ImageBitmap,
    fit: WallpaperFit,
    x: Float,
    y: Float,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap,
        contentDescription = null,
        alignment = BiasAlignment(x * 2f - 1f, y * 2f - 1f),
        contentScale = when (fit) {
            WallpaperFit.FILL -> ContentScale.Crop
            WallpaperFit.FIT -> ContentScale.Fit
            WallpaperFit.STRETCH -> ContentScale.FillBounds
        },
        modifier = modifier,
    )
}

/** The default wallpaper from the design: a deep blue sky with three soft waves. */
@Composable
fun DefaultWallpaper(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sx = w / 1440f
        val sy = h / 720f

        drawRect(
            Brush.linearGradient(
                listOf(Color(0xFF12163A), Color(0xFF1F4A8F)),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x8C6FD6FF), Color(0x006FD6FF)),
                center = Offset(w * 0.82f, h * 0.12f),
                radius = w * 0.45f,
            ),
        )

        val wave1 = Path().apply {
            moveTo(0f, 500f * sy)
            cubicTo(240f * sx, 400f * sy, 480f * sx, 620f * sy, 760f * sx, 500f * sy)
            cubicTo(1040f * sx, 380f * sy, 1200f * sx, 360f * sy, 1440f * sx, 450f * sy)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            wave1,
            Brush.horizontalGradient(listOf(Color(0xFF3B5BDB), Color(0xFF8A4DFF)), startX = 0f, endX = w),
            alpha = 0.85f,
        )

        val wave2 = Path().apply {
            moveTo(0f, 590f * sy)
            cubicTo(300f * sx, 500f * sy, 520f * sx, 690f * sy, 820f * sx, 600f * sy)
            cubicTo(1120f * sx, 510f * sy, 1220f * sx, 500f * sy, 1440f * sx, 575f * sy)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            wave2,
            Brush.horizontalGradient(listOf(Color(0xFF7A4DFF), Color(0xFFFF6AA0)), startX = 0f, endX = w),
            alpha = 0.88f,
        )

        val wave3 = Path().apply {
            moveTo(0f, 655f * sy)
            cubicTo(360f * sx, 600f * sy, 600f * sx, 715f * sy, 900f * sx, 665f * sy)
            cubicTo(1200f * sx, 615f * sy, 1240f * sx, 615f * sy, 1440f * sx, 655f * sy)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            wave3,
            Brush.horizontalGradient(listOf(Color(0xFFFF6AA0), Color(0xFFFFB36A)), startX = 0f, endX = w),
            alpha = 0.92f,
        )
    }
}
