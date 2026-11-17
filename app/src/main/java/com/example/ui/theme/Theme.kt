package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AuvionyxBrandPalette(
    val accentPrimary: Color = ElectricCyan,
    val accentSecondary: Color = IceBlue,
    val accentDeep: Color = DeepBlue,
    val background: Color = ObsidianBlack,
    val glassWhite: Color = GlassWhite,
    val isAdaptiveAlbumMode: Boolean = false
)

@Immutable
data class AuvionyxMotionSpec(
    val reducedMotion: Boolean = false
)

val LocalAuvionyxPalette = staticCompositionLocalOf { AuvionyxBrandPalette() }
val LocalAuvionyxMotion = staticCompositionLocalOf { AuvionyxMotionSpec() }

val AuvionyxShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun AuvionyxTheme(
    adaptiveAccentColor: Color? = null,
    useAdaptiveAlbumColors: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always default to official Electric Cyan / Ice Blue / Deep Blue on pure #000000 AMOLED black
    val activePrimary = if (useAdaptiveAlbumColors && adaptiveAccentColor != null) {
        adaptiveAccentColor
    } else {
        ElectricCyan
    }

    val palette = AuvionyxBrandPalette(
        accentPrimary = activePrimary,
        accentSecondary = IceBlue,
        accentDeep = DeepBlue,
        background = ObsidianBlack,
        glassWhite = GlassWhite,
        isAdaptiveAlbumMode = useAdaptiveAlbumColors && adaptiveAccentColor != null
    )

    val colorScheme = darkColorScheme(
        primary = activePrimary,
        onPrimary = ObsidianBlack,
        primaryContainer = DeepBlue,
        onPrimaryContainer = GlassWhite,
        secondary = IceBlue,
        onSecondary = ObsidianBlack,
        secondaryContainer = DeepBlue.copy(alpha = 0.45f),
        onSecondaryContainer = GlassWhite,
        tertiary = DeepBlue,
        onTertiary = GlassWhite,
        background = ObsidianBlack,
        onBackground = GlassWhite,
        surface = ObsidianBlack,
        onSurface = GlassWhite,
        surfaceVariant = Color(0xFF070B12),
        onSurfaceVariant = TextSecondary,
        outline = GlassBorderSubtle,
        outlineVariant = Color(0x1CFFFFFF),
        error = PulseCongestion,
        onError = GlassWhite
    )

    CompositionLocalProvider(
        LocalAuvionyxPalette provides palette,
        LocalAuvionyxMotion provides AuvionyxMotionSpec(reducedMotion = reducedMotion)
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AuvionyxTypography,
            shapes = AuvionyxShapes,
            content = content
        )
    }
}
