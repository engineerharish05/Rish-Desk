package com.rishdeskdroid.app.ui

import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.rishdeskdroid.app.data.WidgetController

/** Shows one hosted Android app widget in a rounded frame of the given size. */
@Composable
fun WidgetView(
    controller: WidgetController,
    id: Int,
    info: AppWidgetProviderInfo,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .size(width, height)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape),
    ) {
        AndroidView(
            factory = { ctx -> controller.host.createView(ctx, id, info) },
            update = { view ->
                val w = width.value.toInt()
                val h = height.value.toInt()
                view.updateAppWidgetSize(Bundle(), w, h, w, h)
            },
            modifier = Modifier.size(width, height),
        )
    }
}
