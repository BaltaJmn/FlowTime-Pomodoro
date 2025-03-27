package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

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
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        content = content
    )
}