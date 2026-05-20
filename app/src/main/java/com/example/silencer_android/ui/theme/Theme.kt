package com.example.silencer_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    secondary = GoogleGreen,
    onSecondary = Color.White,
    tertiary = GoogleYellow,
    onTertiary = Color.Black,
    error = GoogleRed,
    onError = Color.White,
    background = White,
    onBackground = TextPrimary,
    surface = White,
    onSurface = TextPrimary,
    surfaceVariant = GrayLog,
    onSurfaceVariant = TextSecondary
)

@Composable
fun HagridTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
