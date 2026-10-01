package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * La tarjeta de la app: un tono por encima del fondo, el de la barra de abajo, sin sombra ni borde.
 * La de Material va en el gris más oscuro de la superficie y pesaba más que lo que lleva dentro.
 */
@Composable
fun FlowCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    content = content
)

/** Los segmentados con el borde suave de los chips: el de Material era la línea más fuerte de la pantalla. */
@Composable
fun quietSegmentedColors(): SegmentedButtonColors = SegmentedButtonDefaults.colors(
    activeBorderColor = MaterialTheme.colorScheme.outlineVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant
)
