package com.finoria.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color

fun Modifier.adaptiveBackground() = composed {
    val color = if (isSystemInDarkTheme()) {
        Color(0xFF121212) // Dark Grey
    } else {
        Color(0xFFF2F2F7) // Light System Grouped Background
    }
    this.background(color)
}