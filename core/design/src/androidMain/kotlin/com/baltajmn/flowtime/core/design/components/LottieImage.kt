package com.baltajmn.flowtime.core.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import com.baltajmn.flowtime.core.design.R
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

@Composable
actual fun LottieImage(
    modifier: Modifier,
    animation: LottieAnimation,
    tintColor: Color,
    playing: Boolean
) {
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = tintColor.toArgb(),
            keyPath = arrayOf("**")
        )
    )
    val raw = when (animation) {
        LottieAnimation.LOADING -> R.raw.loading
        LottieAnimation.EQUALIZER -> R.raw.equalizer
    }
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw))
    val animationProgress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = playing,
        iterations = LottieConstants.IterateForever,
        restartOnPlay = false
    )
    LottieAnimation(
        modifier = modifier,
        composition = composition,
        progress = { animationProgress },
        dynamicProperties = dynamicProperties,
        contentScale = ContentScale.Crop
    )
}