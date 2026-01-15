package com.synthio.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SynthColorScheme = lightColorScheme(
    primary = PastelPink,
    onPrimary = TextPrimary,
    primaryContainer = PastelPinkLight,
    onPrimaryContainer = TextPrimary,
    secondary = PastelLavender,
    onSecondary = TextPrimary,
    secondaryContainer = PastelLavenderLight,
    onSecondaryContainer = TextPrimary,
    tertiary = PastelMint,
    onTertiary = TextPrimary,
    tertiaryContainer = PastelMintLight,
    onTertiaryContainer = TextPrimary,
    background = BackgroundCream,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary
)

object SynthShapes {
    val small = RoundedCornerShape(12.dp)
    val medium = RoundedCornerShape(20.dp)
    val large = RoundedCornerShape(28.dp)
    val extraLarge = RoundedCornerShape(36.dp)
    val pill = RoundedCornerShape(50)
}

@Composable
fun SynthioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SynthColorScheme,
        content = content
    )
}
