package com.adso.eggchecker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/** Paleta de marca EggChecker (misma del frontend web). */
private val LightColorScheme = lightColorScheme(
    primary = Brown,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Green,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Yellow,
    onTertiary = Brown,
    background = Cream,
    onBackground = Dark,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = Dark,
    error = ErrorRed,
    onError = androidx.compose.ui.graphics.Color.White,
    outline = Border
)

/**
 * Tema único de la app. No usa color dinámico para conservar la identidad
 * visual de EggChecker en todos los dispositivos.
 */
@Composable
fun EggCheckerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
