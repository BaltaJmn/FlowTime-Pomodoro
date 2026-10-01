package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.LargeTitle

/** El título de una pantalla, a la izquierda y en el color del texto. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.onSurface)
    )
}
