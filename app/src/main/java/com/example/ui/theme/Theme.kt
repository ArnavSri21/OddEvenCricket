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
data class StadiumColors(
    val turfLight: Color = PitchTurfLight,
    val turfDark: Color = PitchTurfDark,
    val pitchSoil: Color = PitchSoil,
    val pitchSoilShadow: Color = PitchSoilShadow,
    val sixGold: Color = FloodlightGold,
    val sixGoldBright: Color = FloodlightGoldBright,
    val sixGoldDeep: Color = FloodlightGoldDeep,
    val sixGoldContainer: Color = FloodlightGoldContainer,
    val fourMint: Color = BoundaryMint,
    val fourMintBright: Color = BoundaryMintBright,
    val fourMintDeep: Color = BoundaryMintDeep,
    val fourMintContainer: Color = BoundaryMintContainer,
    val wicketRed: Color = WicketCrimson,
    val wicketRedBright: Color = WicketCrimsonBright,
    val wicketRedContainer: Color = WicketCrimsonContainer,
    val cardElevated: Color = PavilionCardElevated,
    val borderSubtle: Color = TurfOutlineSubtle,
    val glassTop: Color = GlassTopSurface,
    val glassBottom: Color = GlassBottomSurface,
    val rimTop: Color = SpecularRimTop,
    val rimBottom: Color = SpecularRimBottom,
)

val LocalStadiumColors = staticCompositionLocalOf { StadiumColors() }

private val StadiumDarkColorScheme = darkColorScheme(
    primary = FloodlightGold,
    onPrimary = OnFloodlightGold,
    primaryContainer = FloodlightGoldContainer,
    onPrimaryContainer = FloodlightGold,
    secondary = BoundaryMint,
    onSecondary = OnBoundaryMint,
    secondaryContainer = BoundaryMintContainer,
    onSecondaryContainer = BoundaryMint,
    tertiary = WicketCrimson,
    onTertiary = OnWicketCrimson,
    tertiaryContainer = WicketCrimsonContainer,
    onTertiaryContainer = WicketCrimson,
    background = PitchNightDeep,
    onBackground = ChalkWhite,
    surface = PitchNightSurface,
    onSurface = ChalkWhite,
    surfaceVariant = PavilionCard,
    onSurfaceVariant = TurfMist,
    surfaceContainerLowest = PitchNightDeep,
    surfaceContainerLow = PitchNightSurface,
    surfaceContainer = PavilionCard,
    surfaceContainerHigh = PavilionCardElevated,
    outline = TurfOutline,
    outlineVariant = TurfOutlineSubtle,
    error = WicketCrimson,
    onError = OnWicketCrimson,
    errorContainer = WicketCrimsonContainer,
    onErrorContainer = OnWicketCrimson,
)

private val StadiumShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun OddEvenCricketTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStadiumColors provides StadiumColors()) {
        MaterialTheme(
            colorScheme = StadiumDarkColorScheme,
            typography = Typography,
            shapes = StadiumShapes,
            content = content,
        )
    }
}
