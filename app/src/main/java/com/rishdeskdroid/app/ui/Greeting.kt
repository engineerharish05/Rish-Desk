package com.rishdeskdroid.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.R
import kotlinx.coroutines.delay
import java.util.Calendar

/** The greeting appears once each time the launcher process starts, not on every Home press. */
private var greetingShownThisRun = false

fun greetingText(hourOfDay: Int): String = when (hourOfDay) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..20 -> "Good evening"
    else -> "Good night"
}

/**
 * A welcome card with the Rish Innovations logo. It fades in over the home screen, then goes away
 * by itself after a few seconds, or immediately when tapped.
 */
@Composable
fun GreetingOverlay(enabled: Boolean, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(enabled && !greetingShownThisRun) }
    LaunchedEffect(Unit) {
        if (visible) {
            greetingShownThisRun = true
            delay(4200)
            visible = false
        }
    }
    val animated = LocalAnimations.current
    AnimatedVisibility(
        visible = visible,
        enter = if (animated) fadeIn() + scaleIn(initialScale = 0.94f) else fadeIn(),
        exit = if (animated) fadeOut() + scaleOut(targetScale = 0.97f) else fadeOut(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFF05060F).copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Dismiss greeting",
                ) { visible = false },
            contentAlignment = Alignment.Center,
        ) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xE61C2048), Color(0xE60E1030))),
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))),
                        shape,
                    )
                    .padding(horizontal = 30.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.rish_logo),
                    contentDescription = "Rish Innovations logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.widthIn(max = 280.dp),
                )
                Text(
                    greetingText(remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    "Welcome to RishDesk",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "by Rish Innovations",
                    color = Color(0xFFE9A15A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
