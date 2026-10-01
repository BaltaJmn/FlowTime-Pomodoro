package com.baltajmn.flowtime.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Las animaciones Lottie de la app, en res/raw (Android) y en composeResources/files (iPhone). */
enum class LottieAnimation(val file: String) {
    LOADING("loading.json"),
    EQUALIZER("equalizer.json")
}

/** Una animación Lottie en bucle, de un solo color. */
@Composable
expect fun LottieImage(
    modifier: Modifier = Modifier,
    animation: LottieAnimation,
    tintColor: Color,
    playing: Boolean = true
)
