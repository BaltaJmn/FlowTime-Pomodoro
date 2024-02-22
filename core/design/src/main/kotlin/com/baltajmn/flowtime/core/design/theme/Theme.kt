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
        AppTheme.Green -> GreenColorScheme
        AppTheme.Beige -> BeigeColorScheme
        AppTheme.Grey -> GreyColorScheme
        AppTheme.Purple -> PurpleColorScheme
        AppTheme.Black -> BlackColorScheme
        AppTheme.White -> WhiteColorScheme
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
    primary = Blue,
    secondary = LightBlue,
    tertiary = DarkBlue,
    background = SurfaceBlue,
    onPrimary = LightBlue,
    onSecondary = Blue,
    onTertiary = DarkBlue,
    onBackground = SurfaceBlue
)
val PinkColorScheme = lightColorScheme(
    primary = Pink,
    secondary = LightPink,
    tertiary = DarkPink,
    background = SurfacePink,
    onPrimary = LightPink,
    onSecondary = Pink,
    onTertiary = DarkPink,
    onBackground = SurfacePink
)
val OrangeColorScheme = lightColorScheme(
    primary = Orange,
    secondary = LightOrange,
    tertiary = DarkOrange,
    background = SurfaceOrange,
    onPrimary = LightOrange,
    onSecondary = Orange,
    onTertiary = DarkOrange,
    onBackground = SurfaceOrange
)
val BrownColorScheme = lightColorScheme(
    primary = Brown,
    secondary = LightBrown,
    tertiary = DarkBrown,
    background = SurfaceBrown,
    onPrimary = LightBrown,
    onSecondary = Brown,
    onTertiary = DarkBrown,
    onBackground = SurfaceBrown
)
val OliveColorScheme = lightColorScheme(
    primary = Olive,
    secondary = LightOlive,
    tertiary = DarkOlive,
    background = SurfaceOlive,
    onPrimary = LightOlive,
    onSecondary = Olive,
    onTertiary = DarkOlive,
    onBackground = SurfaceOlive
)
val MarineColorScheme = lightColorScheme(
    primary = Marine,
    secondary = LightMarine,
    tertiary = DarkMarine,
    background = SurfaceMarine,
    onPrimary = LightMarine,
    onSecondary = Marine,
    onTertiary = DarkMarine,
    onBackground = SurfaceMarine
)
val GreenColorScheme = lightColorScheme(
    primary = Green,
    secondary = LightGreen,
    tertiary = DarkGreen,
    background = SurfaceGreen,
    onPrimary = LightGreen,
    onSecondary = Green,
    onTertiary = DarkGreen,
    onBackground = SurfaceGreen
)
val BeigeColorScheme = lightColorScheme(
    primary = Beige,
    secondary = LightBeige,
    tertiary = DarkBeige,
    background = SurfaceBeige,
    onPrimary = LightBeige,
    onSecondary = Beige,
    onTertiary = DarkBeige,
    onBackground = SurfaceBeige
)
val GreyColorScheme = lightColorScheme(
    primary = Grey,
    secondary = LightGrey,
    tertiary = DarkGrey,
    background = SurfaceGrey,
    onPrimary = LightGrey,
    onSecondary = Grey,
    onTertiary = DarkGrey,
    onBackground = SurfaceGrey
)
val PurpleColorScheme = lightColorScheme(
    primary = Purple,
    secondary = LightPurple,
    tertiary = DarkPurple,
    background = SurfacePurple,
    onPrimary = LightPurple,
    onSecondary = Purple,
    onTertiary = DarkPurple,
    onBackground = SurfacePurple
)
val BlackColorScheme = lightColorScheme(
    primary = Black,
    secondary = LightBlack,
    tertiary = DarkBlack,
    background = SurfaceBlack,
    onPrimary = LightBlack,
    onSecondary = Black,
    onTertiary = DarkBlack,
    onBackground = SurfaceBlack
)
val WhiteColorScheme = lightColorScheme(
    primary = White,
    secondary = LightWhite,
    tertiary = DarkWhite,
    background = SurfaceWhite,
    onPrimary = LightWhite,
    onSecondary = White,
    onTertiary = DarkWhite,
    onBackground = SurfaceWhite
)