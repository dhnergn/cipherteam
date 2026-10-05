package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SecureDarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    secondary = CyberBlue,
    tertiary = CyberPurple,
    background = SpaceDark,
    surface = SlateGrey,
    onPrimary = SpaceDark,
    onSecondary = SpaceDark,
    onTertiary = TextLight,
    onBackground = TextLight,
    onSurface = TextLight,
    error = SecureRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force secure dark-theme style by default for professional cyberpunk vibe
    dynamicColor: Boolean = false, // Disable dynamic colors to keep our crisp intentional cyber theme
    content: @Composable () -> Unit,
) {
    val colorScheme = SecureDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
