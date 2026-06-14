package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OnyxColorScheme = lightColorScheme(
    primary = NeonTeal,
    onPrimary = Color.White,
    secondary = CyberAmber,
    onSecondary = Color(0xFF001D36),
    tertiary = TerminalGreen,
    onTertiary = Color.White,
    background = RichOnyx,
    onBackground = WarmWhite,
    surface = CardBackground,
    onSurface = WarmWhite,
    surfaceVariant = DeepSlate,
    onSurfaceVariant = SoftMutedGray,
    outline = BorderDark,
    error = PhantomRed,
    onError = Color.White
)

@Composable
fun AetherisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OnyxColorScheme,
        typography = Typography,
        content = content
    )
}
