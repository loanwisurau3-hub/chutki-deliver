package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FleetDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = SkyInfo,
    onSecondary = Color(0xFF04101D),
    secondaryContainer = Color(0xFF0E2A47),
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    background = TerminalBg,
    onBackground = TextPrimary,
    surface = TerminalSurface,
    onSurface = TextPrimary,
    surfaceVariant = TerminalCard,
    onSurfaceVariant = TextMuted,
    outline = TerminalBorder,
    error = CrimsonDanger,
    onError = Color.White,
    errorContainer = Color(0xFF3F1218),
    onErrorContainer = CrimsonLight
)

val FleetShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GossaigaonFleetTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FleetDarkColorScheme,
        typography = FleetTypography,
        shapes = FleetShapes,
        content = content
    )
}
