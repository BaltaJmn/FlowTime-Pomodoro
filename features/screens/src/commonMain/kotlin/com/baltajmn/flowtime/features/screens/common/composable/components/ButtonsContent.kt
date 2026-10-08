package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import org.jetbrains.compose.resources.stringResource
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.painterResource

private fun TimerAction.icon() = when (this) {
    TimerAction.START, TimerAction.RESUME -> Res.drawable.ic_play
    TimerAction.STOP -> Res.drawable.ic_stop
    TimerAction.PAUSE -> Res.drawable.ic_pause
    TimerAction.BREAK, TimerAction.SKIP_BREAK -> Res.drawable.ic_next
}

/** Empezar, pausar o seguir: lo que se pulsa casi siempre. Parar y saltar, en segundo plano. */
private val TimerAction.primary get() = this == TimerAction.START || this == TimerAction.RESUME || this == TimerAction.PAUSE

/**
 * Las acciones de la sesión, cada una con su nombre debajo: tres círculos iguales solo se
 * distinguían por el icono, y parar pesaba lo mismo que pausar. Cada una en su sitio fijo: parar a
 * la izquierda, la principal en el centro y el descanso a la derecha, en todos los modos.
 */
@Composable
fun ButtonsContent(
    state: FocusUiState,
    onAction: (TimerAction) -> Unit
) {
    val actions = state.actions
    val slots = listOf(
        actions.firstOrNull { it == TimerAction.STOP },
        actions.firstOrNull { it.primary },
        actions.firstOrNull { it == TimerAction.BREAK || it == TimerAction.SKIP_BREAK }
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        slots.forEach { action ->
            if (action == null) Spacer(modifier = Modifier.width(SLOT)) else ActionButton(action, onAction)
        }
    }
}

@Composable
private fun ActionButton(action: TimerAction, onAction: (TimerAction) -> Unit) {
    val haptics = LocalHapticFeedback.current
    val label = stringResource(action.label)
    Column(
        modifier = Modifier.width(SLOT),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Del mismo alto todos, para que los nombres queden en línea.
        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            CircularButton(
                tonal = !action.primary,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction(action)
                }
            ) {
                Icon(painter = painterResource(action.icon()), contentDescription = label)
            }
        }
        // El botón ya se anuncia con su nombre: el texto es solo para verlo.
        Text(
            text = label,
            modifier = Modifier
                .padding(top = 6.dp)
                .clearAndSetSemantics {},
            style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

private val SLOT = 104.dp

@Preview(showBackground = true)
@Composable
fun ButtonsContentRunningPreview() {
    ButtonsContent(
        state = FocusUiState(mode = TimerMode.FLOW_TIME, phase = Phase.WORK),
        onAction = {}
    )
}
