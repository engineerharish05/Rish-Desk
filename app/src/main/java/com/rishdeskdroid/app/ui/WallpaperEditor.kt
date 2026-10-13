package com.rishdeskdroid.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.max
import kotlin.math.min

private const val MAX_ZOOM = 5f
private const val MAX_OUTPUT_EDGE = 2560

/**
 * Full-screen wallpaper editor. The photo fills the screen exactly as it will on the home screen.
 * Drag with one finger to move it, pinch to zoom, then tap Set. [onSet] receives a bitmap that is
 * already cropped to the screen shape, so what you see here is what you get.
 */
@Composable
fun WallpaperEditor(
    photo: Bitmap,
    onSet: (Bitmap) -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        var area by remember { mutableStateOf(IntSize.Zero) }
        var zoom by remember { mutableFloatStateOf(1f) }   // 1 = photo just covers the screen
        var offset by remember { mutableStateOf(Offset.Zero) } // photo centre relative to screen centre
        val image = remember(photo) { photo.asImageBitmap() }

        // Scale at which the photo exactly covers the screen
        fun cover(): Float = if (area.width == 0 || area.height == 0) 1f else
            max(area.width / photo.width.toFloat(), area.height / photo.height.toFloat())

        fun clamp(o: Offset, z: Float): Offset {
            val s = cover() * z
            val maxX = max(0f, (photo.width * s - area.width) / 2f)
            val maxY = max(0f, (photo.height * s - area.height) / 2f)
            return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
        }

        fun zoomTo(newZoom: Float, focus: Offset = Offset.Zero) {
            val z = newZoom.coerceIn(1f, MAX_ZOOM)
            val factor = z / zoom
            // Keep the point under the fingers where it is while zooming
            offset = clamp(focus + (offset - focus) * factor, z)
            zoom = z
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .semantics { contentDescription = "Wallpaper editor. Drag to move the photo, pinch to zoom." },
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        area = it
                        offset = clamp(offset, zoom)
                    }
                    .pointerInput(photo) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            if (area.width == 0) return@detectTransformGestures
                            val focus = centroid - Offset(area.width / 2f, area.height / 2f)
                            val z = (zoom * gestureZoom).coerceIn(1f, MAX_ZOOM)
                            val factor = z / zoom
                            offset = clamp(focus + (offset - focus) * factor + pan, z)
                            zoom = z
                        }
                    },
            ) {
                val s = cover() * zoom
                withTransform({
                    translate(size.width / 2f + offset.x, size.height / 2f + offset.y)
                    scale(s, s, pivot = Offset.Zero)
                }) {
                    drawImage(image, topLeft = Offset(-photo.width / 2f, -photo.height / 2f))
                }
            }

            Text(
                "Drag to move  ·  Pinch to zoom",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                style = LabelStyle,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 22.dp)
                    .background(Color(0xCC12142E), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton("−", "Zoom out", zoom > 1.001f) { zoomTo(zoom / 1.25f) }
                StepButton("+", "Zoom in", zoom < MAX_ZOOM - 0.001f) { zoomTo(zoom * 1.25f) }
                GlassButton("Reset", enabled = zoom != 1f || offset != Offset.Zero) {
                    zoom = 1f
                    offset = Offset.Zero
                }
                GlassButton("Cancel", onClick = onCancel)
                GlassButton("Set", primary = true) {
                    onSet(renderCrop(photo, area, zoom, offset, cover()))
                }
            }
        }
    }
}

/** Cuts exactly the part of the photo that is visible on screen into a new, screen-shaped bitmap. */
private fun renderCrop(photo: Bitmap, area: IntSize, zoom: Float, offset: Offset, cover: Float): Bitmap {
    val w = max(1, area.width)
    val h = max(1, area.height)
    val s = cover * zoom
    // Region of the photo (in photo pixels) that is on screen
    val centerX = photo.width / 2f - offset.x / s
    val centerY = photo.height / 2f - offset.y / s
    val halfW = w / (2f * s)
    val halfH = h / (2f * s)
    val src = RectF(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
    // Stay inside the photo despite rounding
    val dx = max(0f, -src.left) - max(0f, src.right - photo.width)
    val dy = max(0f, -src.top) - max(0f, src.bottom - photo.height)
    src.offset(dx, dy)
    val srcRect = Rect(
        max(0, src.left.toInt()),
        max(0, src.top.toInt()),
        min(photo.width, src.right.toInt()),
        min(photo.height, src.bottom.toInt()),
    )

    val outScale = min(1f, MAX_OUTPUT_EDGE / max(w, h).toFloat())
    val outW = max(1, (w * outScale).toInt())
    val outH = max(1, (h * outScale).toInt())
    val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    AndroidCanvas(out).drawBitmap(photo, srcRect, Rect(0, 0, outW, outH), paint)
    return out
}
