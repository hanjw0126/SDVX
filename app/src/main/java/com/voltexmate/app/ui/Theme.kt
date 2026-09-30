package com.voltexmate.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Mochi {
    val Lavender = Color(0xFF8C7BFF)
    val Pink = Color(0xFFFF8FC7)
    val Mint = Color(0xFF6FD9B8)
    val Sky = Color(0xFF7CC8FF)
    val Ink = Color(0xFF2D2A4A)
    val Sub = Color(0xFF8A86A8)
    val Bg = Color(0xFFF7F4FF)
}

internal fun Color.darker(f: Float = 0.72f): Color = Color(red * f, green * f, blue * f, alpha)

@Composable
fun VoltexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Mochi.Lavender,
            onPrimary = Color.White,
            secondary = Mochi.Pink,
            background = Mochi.Bg,
            onBackground = Mochi.Ink,
            surface = Color.White,
            onSurface = Mochi.Ink,
        ),
        content = content,
    )
}
