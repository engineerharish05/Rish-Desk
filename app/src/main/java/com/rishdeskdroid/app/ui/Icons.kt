package com.rishdeskdroid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.PathParser

/** An icon on a 24 x 24 grid, described by SVG path data (the same icons used in the design). */
class IconDef(
    val strokes: List<String> = emptyList(),
    val fills: List<String> = emptyList(),
)

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0z"

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float): String =
    "M${x + r},${y}h${w - 2 * r}a$r,$r 0 0,1 $r,${r}v${h - 2 * r}" +
        "a$r,$r 0 0,1 ${-r},${r}h${-(w - 2 * r)}a$r,$r 0 0,1 ${-r},${-r}" +
        "v${-(h - 2 * r)}a$r,$r 0 0,1 $r,${-r}z"

object Ico {
    val gear = IconDef(
        strokes = listOf(
            circle(12f, 12f, 3.6f),
            "M12 2.5v3.2M12 18.3v3.2M2.5 12h3.2M18.3 12h3.2M5.3 5.3l2.3 2.3M16.4 16.4l2.3 2.3" +
                "M5.3 18.7l2.3-2.3M16.4 7.6l2.3-2.3",
        ),
    )
    val bluetooth = IconDef(strokes = listOf("M6.5 6.5l11 11L12 23V1l5.5 5.5-11 11"))
    val wifi = IconDef(
        strokes = listOf("M2 9a15 15 0 0 1 20 0", "M5.5 12.5a10 10 0 0 1 13 0", "M9 16a5 5 0 0 1 6 0"),
        fills = listOf(circle(12f, 19.5f, 1.1f)),
    )
    val plus = IconDef(strokes = listOf("M12 5v14M5 12h14"))
    val close = IconDef(strokes = listOf("M6 6l12 12M18 6L6 18"))
    val grid = IconDef(
        fills = List(9) { i ->
            rrect(3.5f + (i % 3) * 6.2f, 3.5f + (i / 3) * 6.2f, 4.6f, 4.6f, 1.2f)
        },
    )
    val exit = IconDef(
        strokes = listOf(
            "M10 4.5H6.5a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2H10",
            "M14.5 8l4.2 4-4.2 4M18.7 12H9.5",
        ),
    )
    val sliders = IconDef(
        strokes = listOf("M4 7h16M4 12h16M4 17h16"),
        fills = listOf(circle(9f, 7f, 2.3f), circle(15.5f, 12f, 2.3f), circle(8f, 17f, 2.3f)),
    )
    val widgets = IconDef(
        strokes = listOf(
            rrect(4f, 4f, 7f, 7f, 2f),
            rrect(13f, 4f, 7f, 7f, 2f),
            rrect(4f, 13f, 7f, 7f, 2f),
            rrect(13f, 13f, 7f, 7f, 2f),
        ),
    )
    val clock = IconDef(strokes = listOf(circle(12f, 12f, 9f), "M12 7v5l3 2"))
    val usb = IconDef(strokes = listOf("M9 3v5M15 3v5M7 8h10v4a5 5 0 0 1-10 0z", "M12 17v4"))
    val keyboard = IconDef(
        strokes = listOf(rrect(2.5f, 6f, 19f, 12f, 3f), "M7 14h10"),
        fills = listOf(
            circle(6.5f, 10f, 0.9f),
            circle(10f, 10f, 0.9f),
            circle(14f, 10f, 0.9f),
            circle(17.5f, 10f, 0.9f),
        ),
    )
    val drop = IconDef(strokes = listOf("M12 3.5s6 6.2 6 10.5a6 6 0 0 1-12 0c0-4.3 6-10.5 6-10.5z"))
    val image = IconDef(
        strokes = listOf(
            rrect(3.5f, 4.5f, 17f, 15f, 3f),
            circle(9f, 10f, 1.6f),
            "M4 17l5-4.5 4 3.5 3-2.5 4 3.5",
        ),
    )
}

@Composable
fun SvgIcon(
    def: IconDef,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    strokeWidth: Float = 1.9f,
) {
    val parsed = remember(def) {
        Pair(
            def.fills.map { PathParser.createPathFromPathData(it).asComposePath() },
            def.strokes.map { PathParser.createPathFromPathData(it).asComposePath() },
        )
    }
    Canvas(modifier.size(iconSize)) {
        val factor = this.size.minDimension / 24f
        scale(scale = factor, pivot = Offset.Zero) {
            parsed.first.forEach { drawPath(it, tint) }
            parsed.second.forEach {
                drawPath(
                    it,
                    tint,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}
