package com.rishdeskdroid.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rishdeskdroid.app.R
import kotlinx.coroutines.delay

/** Matches the system splash background (see values-v31/themes.xml) so the two blend together. */
val SplashBackground = Color(0xFF05060A)

/** The opening screen only appears when the launcher process starts, never on a Home press. */
private var splashShownThisRun = false

/**
 * The opening screen: the RishDesk icon in the middle and "Made by" with the Rish Innovations logo
 * at the bottom centre. It fades away by itself after about a second and a half.
 */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(!splashShownThisRun) }
    LaunchedEffect(Unit) {
        if (visible) {
            splashShownThisRun = true
            delay(1500)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(SplashBackground)
                // Decorative: nothing here should be read out or tapped
                .clearAndSetSemantics { },
        ) {
            // App icon: the cream tile with the RD letters, as on the launcher
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(120.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xFFFEFFEF)),
                contentAlignment = Alignment.Center,
            ) {
                // The icon artwork is drawn on a 108dp canvas of which the middle 72dp is visible
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.requiredSize(180.dp),
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Made by",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                )
                Image(
                    painter = painterResource(R.drawable.rish_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .widthIn(max = 170.dp),
                )
            }
        }
    }
}
