package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.google.accompanist.systemuicontroller.rememberSystemUiController


@Composable
fun FlowTimeTheme(
    appTheme: AppTheme,
    content: @Composable () -> Unit
) {

    val colorScheme = when (appTheme) {
        AppTheme.Blue -> BlueColorScheme
        AppTheme.Pink -> PinkColorScheme
        AppTheme.Orange -> OrangeColorScheme
        AppTheme.Brown -> BrownColorScheme
        AppTheme.Olive -> OliveColorScheme
        AppTheme.Marine -> MarineColorScheme
        AppTheme.Mint -> MintColorScheme
    }

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = colorScheme.background.copy(alpha = 0.5f),
        darkIcons = false
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        content = content
    )
}

val BlueColorScheme = lightColorScheme(
    primary = LightBlue,
    secondary = Blue,
    tertiary = DarkBlue,
    background = SurfaceBlue,
    onPrimary = LightBlue,
    onSecondary = Blue,
    onTertiary = DarkBlue,
    onBackground = SurfaceBlue
)
val PinkColorScheme = lightColorScheme(
    primary = LightPink,
    secondary = Pink,
    tertiary = DarkPink,
    background = SurfacePink,
    onPrimary = LightPink,
    onSecondary = Pink,
    onTertiary = DarkPink,
    onBackground = SurfacePink
)
val OrangeColorScheme = lightColorScheme(
    primary = LightOrange,
    secondary = Orange,
    tertiary = DarkOrange,
    background = SurfaceOrange,
    onPrimary = LightOrange,
    onSecondary = Orange,
    onTertiary = DarkOrange,
    onBackground = SurfaceOrange
)
val BrownColorScheme = lightColorScheme(
    primary = LightBrown,
    secondary = Brown,
    tertiary = DarkBrown,
    background = SurfaceBrown,
    onPrimary = LightBrown,
    onSecondary = Brown,
    onTertiary = DarkBrown,
    onBackground = SurfaceBrown
)
val OliveColorScheme = lightColorScheme(
    primary = LightOlive,
    secondary = Olive,
    tertiary = DarkOlive,
    background = SurfaceOlive,
    onPrimary = LightOlive,
    onSecondary = Olive,
    onTertiary = DarkOlive,
    onBackground = SurfaceOlive
)
val MarineColorScheme = lightColorScheme(
    primary = LightMarine,
    secondary = Marine,
    tertiary = DarkMarine,
    background = SurfaceMarine,
    onPrimary = LightMarine,
    onSecondary = Marine,
    onTertiary = DarkMarine,
    onBackground = SurfaceMarine
)
val MintColorScheme = lightColorScheme(
    primary = LightMint,
    secondary = Mint,
    tertiary = DarkMint,
    background = SurfaceMint,
    onPrimary = LightMint,
    onSecondary = Mint,
    onTertiary = DarkMint,
    onBackground = SurfaceMint
)
