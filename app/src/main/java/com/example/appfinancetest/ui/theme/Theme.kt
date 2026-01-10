package com.example.appfinancetest.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    onPrimary = Color.White,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    
    // Contrast for cards
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceContainer = Color(0xFF121212), // Color for dialogs/popups identical to background
    onSurface = Color.White,

    surfaceVariant = Color(0xFF00C853), // Light green (Positive text)
    error = Color(0xFF8B0000)        // Dark red (Negative text)
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.Black,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    // Contrast for cards
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF5F5F5), // Color for dialogs/popups identical to background
    onSurface = Color.Black,

    surfaceVariant = Color(0xFF008000), // Dark green (Positive text)
    error = Color(0xFF8B0000)        // Dark red (Negative text)
)

@Composable
fun AppFinanceTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}