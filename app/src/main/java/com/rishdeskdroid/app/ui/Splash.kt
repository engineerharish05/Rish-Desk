package com.rishdeskdroid.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
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
 * The opening screen: the RishDesk icon centred, and "Made by" with the Rish Innovations logo at the
 * bottom centre. The two never overlap: the icon is centred in the space left above the branding, and
 * every size scales down on short (landscape) screens. It fades away by itself after about two seconds.
 */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(!splashShownThisRun) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (visible) {
            splashShownThisRun = true
            entered = true
            delay(2000)
            visible = false
        }
    }
    val iconAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(600), label = "splashIcon")
    val iconScale by animateFloatAsState(if (entered) 1f else 0.9f, tween(700), label = "splashScale")
    val brandAlpha by animateFloatAsState(
        if (entered) 1f else 0f,
        tween(600, delayMillis = 350),
        label = "splashBrand",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(400)),
        modifier = modifier,
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(SplashBackground)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF111628), Color.Transparent),
                    ),
                )
                // Decorative: nothing here should be read out or tapped
                .clearAndSetSemantics { },
        ) {
            // Short screens (phones in landscape) get smaller artwork so nothing collides
            val compact = maxHeight < 480.dp
            val tile = if (compact) 92.dp else 128.dp
            val logoHeight = if (compact) 34.dp else 48.dp

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Icon: takes the space above the branding and centres itself in it
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .graphicsLayer {
                                alpha = iconAlpha
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                            .size(tile)
                            .clip(RoundedCornerShape(tile * 0.24f))
                            .background(Color(0xFFFEFFEF)),
                        contentAlignment = Alignment.Center,
                    ) {
                        // The artwork is drawn on a 108dp canvas of which the middle 72dp is visible;
                        // 1.75x leaves a comfortable margin around the letters inside the tile
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.requiredSize(tile * 1.75f),
                        )
                    }
                }

                // Branding: always sits at the bottom centre, below the icon area
                Column(
                    Modifier
                        .graphicsLayer { alpha = brandAlpha }
                        .padding(bottom = if (compact) 16.dp else 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "MADE BY",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = if (compact) 9.sp else 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp,
                    )
                    Image(
                        painter = painterResource(R.drawable.rish_logo),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(top = if (compact) 4.dp else 8.dp)
                            .height(logoHeight),
                    )
                }
            }
        }
    }
}
