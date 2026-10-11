package com.rishdeskdroid.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.data.StatusController
import com.rishdeskdroid.app.data.StatusState

val StatusBarHeight = 24.dp

/** Keeps the status state live while it is in the composition. */
@Composable
fun rememberStatus(): StatusState {
    val context = LocalContext.current
    val controller = remember { StatusController(context) }
    DisposableEffect(controller) {
        controller.start()
        onDispose { controller.stop() }
    }
    return controller.state
}

/**
 * The custom status bar from the design: carrier and time on the left; Bluetooth, Wi-Fi,
 * network signal and battery on the right. Bluetooth, Wi-Fi and signal only show when available.
 */
@Composable
fun StatusBar(
    status: StatusState,
    opacity: Float,
    showDate: Boolean,
    onWifiClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onBatteryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = Color(0xFF0E1028).copy(alpha = 0.18f + 0.23f * opacity.coerceIn(0f, 1f))
    Row(
        modifier
            .fillMaxWidth()
            .height(StatusBarHeight)
            .background(background)
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.14f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.carrier.isNotBlank()) {
            Text(status.carrier, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .size(width = 1.dp, height = 11.dp)
                    .background(Color.White.copy(alpha = 0.45f)),
            )
        }
        Text(status.time, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        if (showDate && status.date.isNotBlank()) {
            Text(
                status.date,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status.bluetoothOn) {
                Box(
                    Modifier
                        .clickable(onClick = onBluetoothClick)
                        .semantics { contentDescription = "Bluetooth on. Open Bluetooth settings" },
                ) { SvgIcon(Ico.bluetooth, 13.dp, strokeWidth = 2.2f) }
            }
            if (status.wifiOn) {
                Box(
                    Modifier
                        .clickable(onClick = onWifiClick)
                        .semantics { contentDescription = "Wi-Fi connected. Open Wi-Fi settings" },
                ) { SvgIcon(Ico.wifi, 15.dp, strokeWidth = 2.2f) }
            }
            if (status.hasCellular) SignalBars(status.signalLevel)
            Row(
                Modifier
                    .clickable(onClick = onBatteryClick)
                    .semantics { contentDescription = "Battery ${status.batteryPercent} percent. Open battery settings" },
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BatteryIcon(status.batteryPercent, status.charging)
                Text(
                    "${status.batteryPercent}%",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SignalBars(level: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 14.dp, height = 10.dp)) {
        val unit = size.width / 7f
        for (i in 0 until 4) {
            val barHeight = size.height * (0.35f + 0.2167f * i)
            drawRoundRect(
                color = Color.White.copy(alpha = if (i < level) 1f else 0.35f),
                topLeft = Offset(i * 2 * unit, size.height - barHeight),
                size = Size(unit, barHeight),
                cornerRadius = CornerRadius(unit / 3f),
            )
        }
    }
}

@Composable
private fun BatteryIcon(percent: Int, charging: Boolean, modifier: Modifier = Modifier) {
    val fillColor = when {
        charging -> Color(0xFF34C759)
        percent <= 15 -> Color(0xFFFF453A)
        else -> Color.White
    }
    Canvas(modifier.size(width = 22.dp, height = 11.dp)) {
        val w = size.width
        val h = size.height
        val body = w - 2.5.dp.toPx()
        val line = 1.2.dp.toPx()
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(line / 2f, line / 2f),
            size = Size(body - line, h - line),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = Stroke(width = line),
        )
        val inset = line + 1.dp.toPx()
        val fillWidth = (body - 2f * inset) * (percent.coerceIn(0, 100) / 100f)
        if (fillWidth > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(inset, inset),
                size = Size(fillWidth, h - 2f * inset),
                cornerRadius = CornerRadius(1.dp.toPx()),
            )
        }
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(body + 0.6.dp.toPx(), h * 0.3f),
            size = Size(1.5.dp.toPx(), h * 0.4f),
            cornerRadius = CornerRadius(0.75.dp.toPx()),
        )
    }
}
