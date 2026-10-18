package com.hoangtu.aistorystudio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val StudioLightColors = lightColorScheme(
    primary = StudioPurple,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFEAE2FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF24134E),
    secondary = StudioWarm,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDBCA),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF351000),
    background = StudioCanvas,
    onBackground = StudioInk,
    surface = androidx.compose.ui.graphics.Color(0xFFFFFBFF),
    onSurface = StudioInk,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF4D4659),
    outline = androidx.compose.ui.graphics.Color(0xFF7E7789)
)

private val StudioDarkColors = darkColorScheme(
    primary = StudioPurpleLight,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF37275F),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF4F3D78),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFEAE2FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFF2B99B),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF4E2A17),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF693F2B),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDBCA),
    background = StudioDarkCanvas,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE9E1EF),
    surface = StudioDarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE9E1EF),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCEC5D7),
    outline = androidx.compose.ui.graphics.Color(0xFF978EA0)
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AIStoryStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> StudioDarkColors
        else -> StudioLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = StudioShapes,
        content = content
    )
}
