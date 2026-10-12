package com.rishdeskdroid.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Soft shadow that keeps white text readable over any wallpaper. */
val TextShadow = Shadow(color = Color(0x99050614), offset = Offset(0f, 2f), blurRadius = 6f)

val LabelStyle = TextStyle(shadow = TextShadow)

fun glassFillAlpha(opacity: Float): Float = 0.08f + 0.32f * opacity.coerceIn(0f, 1f)

/**
 * The frosted-glass surface used by the dock, the settings card and the status bar.
 * [opacity] is the user's "Glass Opacity" setting.
 */
fun Modifier.glass(shape: Shape, opacity: Float, dark: Boolean = false): Modifier {
    val o = opacity.coerceIn(0f, 1f)
    val fill = if (dark) {
        Color(0xFF0E1028).copy(alpha = 0.18f + 0.23f * o)
    } else {
        Color.White.copy(alpha = glassFillAlpha(o))
    }
    val edge = Color.White.copy(alpha = if (dark) 0.16f else 0.34f)
    return this
        .shadow(
            elevation = 12.dp,
            shape = shape,
            clip = false,
            ambientColor = Color(0x66050614),
            spotColor = Color(0x66050614),
        )
        .clip(shape)
        .background(fill)
        .border(1.dp, edge, shape)
}

/** Dashed rounded outline, used for the "+" add-app tile. */
fun Modifier.dashedBorder(color: Color, radius: Dp, width: Dp = 1.5.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(w / 2, w / 2),
        size = Size(this.size.width - w, this.size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
    )
}

/** Gently scales the element up while it is pressed. */
fun Modifier.pressScale(source: MutableInteractionSource, to: Float = 1.12f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val animated = LocalAnimations.current
    val s by animateFloatAsState(if (pressed && animated) to else 1f, label = "pressScale")
    graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

object Tiles {
    val gray = Brush.verticalGradient(listOf(Color(0xFFA6ACB8), Color(0xFF5A606C)))
    val graphite = Brush.verticalGradient(listOf(Color(0xFF8D90A0), Color(0xFF4A4C5A)))
    val blue = Brush.verticalGradient(listOf(Color(0xFF6A96FF), Color(0xFF3650D8)))
    val red = Brush.verticalGradient(listOf(Color(0xFFFF8A63), Color(0xFFDF3F4A)))
    val green = Brush.verticalGradient(listOf(Color(0xFF63D98E), Color(0xFF1F9F52)))
    val sky = Brush.verticalGradient(listOf(Color(0xFF5AC8FA), Color(0xFF2F6DF0)))
    val pink = Brush.verticalGradient(listOf(Color(0xFFFFA3C8), Color(0xFFFF6A5A)))
    val glass = Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x2EFFFFFF)))
}

/** A rounded-square tile (dock buttons). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconTile(
    size: Dp,
    brush: Brush,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    decoration: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(size * 0.24f)
    Box(
        modifier
            .size(size)
            .pressScale(source)
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(brush)
            .then(decoration)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** ON / OFF switch from the settings design. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
    val track = if (checked) Color(0xFF34C759) else Color.White.copy(alpha = 0.3f)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (checked) "ON" else "OFF",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(track),
        ) {
            Box(
                Modifier
                    .offset(x = knobX, y = 2.dp)
                    .size(24.dp)
                    .shadow(2.dp, CircleShape)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
