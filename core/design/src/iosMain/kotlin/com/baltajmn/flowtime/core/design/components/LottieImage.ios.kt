package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.baltajmn.flowtime.core.design.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

/** En el iPhone, con Compottie: lee el mismo JSON que Android, de composeResources/files. */
@OptIn(ExperimentalCompottieApi::class)
@Composable
actual fun LottieImage(
    modifier: Modifier,
    animation: LottieAnimation,
    tintColor: Color,
    playing: Boolean
) {
    val composition by rememberLottieComposition(animation) {
        LottieCompositionSpec.JsonString(Res.readBytes("files/${animation.file}").decodeToString())
    }
    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = playing,
        iterations = Compottie.IterateForever,
        restartOnPlay = false
    )
    // Como en Android: todas las formas, del color que se pide.
    val tint = rememberLottieDynamicProperties(tintColor) {
        shapeLayer("**") {
            fill("**") { color { tintColor } }
            stroke("**") { color { tintColor } }
        }
    }
    Image(
        painter = rememberLottiePainter(composition, progress = { progress }, dynamicProperties = tint),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
