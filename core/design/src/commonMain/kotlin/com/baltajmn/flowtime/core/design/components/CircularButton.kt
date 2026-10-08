package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Un botón redondo y plano, como el resto de superficies. El lleno es la acción principal; [tonal],
 * las que no se deben pulsar sin querer (parar, saltar), más pequeñas y en el tono suave.
 */
@Composable
fun CircularButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    size: Dp = if (tonal) 64.dp else 80.dp,
    content: @Composable () -> Unit
) {
    val shaped = modifier.size(size)
    if (tonal) {
        FilledTonalButton(
            onClick = onClick,
            modifier = shaped,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp)
        ) { content() }
    } else {
        Button(
            onClick = onClick,
            modifier = shaped,
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
        ) { content() }
    }
}
