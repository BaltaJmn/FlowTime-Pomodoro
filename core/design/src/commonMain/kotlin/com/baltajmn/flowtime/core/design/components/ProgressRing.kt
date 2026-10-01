package com.baltajmn.flowtime.core.design.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Un anillo de progreso, de 0 a 1, con lo que se quiera dentro. Avanza suave de un valor al
 * siguiente (de segundo en segundo, en el temporizador), y salta sin animar cuando el cambio es
 * grande, como al pasar del trabajo al descanso. Lo usan el temporizador y el objetivo diario.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    strokeWidth: Dp = 12.dp,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (abs(target - animated.value) > JUMP) {
            animated.snapTo(target)
        } else {
            animated.animateTo(target, tween(durationMillis = 1000, easing = LinearEasing))
        }
    }
    Box(
        modifier = modifier
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f) }
            .drawBehind {
                val stroke = strokeWidth.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                val arc = Size(diameter, diameter)
                drawArc(trackColor, 0f, 360f, useCenter = false, topLeft, arc, style = Stroke(stroke))
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arc,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

private const val JUMP = 0.5f
