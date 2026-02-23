package com.finoria.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = Grey900,
    primaryContainer = Blue700,
    onPrimaryContainer = Blue200,
    secondary = Green200,
    onSecondary = Grey900,
    secondaryContainer = Green700,
    onSecondaryContainer = Green200,
    tertiary = Purple200,
    error = Red200,
    background = Grey900,
    onBackground = Grey50,
    surface = Grey900,
    onSurface = Grey50,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey200
)

private val LightColorScheme = lightColorScheme(
    primary = Blue500,
    onPrimary = Grey50,
    primaryContainer = Blue200,
    onPrimaryContainer = Blue700,
    secondary = Green500,
    onSecondary = Grey50,
    secondaryContainer = Green200,
    onSecondaryContainer = Green700,
    tertiary = Purple500,
    error = Red500,
    background = Grey50,
    onBackground = Grey900,
    surface = Grey50,
    onSurface = Grey900,
    surfaceVariant = Grey100,
    onSurfaceVariant = Grey600
)

@Composable
fun FinoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
