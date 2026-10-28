package com.casasegura.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CasaSeguraDark = darkColorScheme(
    primary = Color(0xFF64D2FF),
    onPrimary = Color(0xFF002B36),
    background = Color.Black,
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF3A3A3C),
)

@Composable
fun CasaSeguraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CasaSeguraDark, content = content)
}
