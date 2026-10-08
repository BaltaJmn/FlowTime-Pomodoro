package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.timer.TimerHint
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

/** La pista bajo el tiempo: el siguiente tramo de FlowTime o el descanso ganado en Porcentaje. */
@Composable
fun TimerHintText(hint: TimerHint?, modifier: Modifier = Modifier) {
    val text = when (hint) {
        is TimerHint.NextStep -> stringResource(
            Res.string.timer_hint_next_step,
            hint.atMinutes.formatMinutesStudying(),
            hint.breakMinutes.formatMinutesStudying()
        )
        is TimerHint.Earned -> stringResource(
            Res.string.timer_hint_earned,
            hint.breakSeconds.formatSecondsToTime()
        )
        null -> return
    }
    // Dentro del anillo: con la letra grande, encoge en vez de salirse.
    Text(
        text = text,
        modifier = modifier,
        style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 14.sp)
    )
}
