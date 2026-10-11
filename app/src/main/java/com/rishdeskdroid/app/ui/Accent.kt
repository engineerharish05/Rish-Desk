package com.rishdeskdroid.app.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** The accent colour chosen in Settings. */
val LocalAccent = compositionLocalOf { Color(0xFF3B82F6) }

/** False when the user turned animations off in Settings. */
val LocalAnimations = compositionLocalOf { true }
