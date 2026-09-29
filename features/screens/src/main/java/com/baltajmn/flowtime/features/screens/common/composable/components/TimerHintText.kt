package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.timer.TimerHint

/** La pista bajo el tiempo: el siguiente tramo de FlowTime o el descanso ganado en Porcentaje. */
@Composable
fun TimerHintText(hint: TimerHint?) {
    val text = when (hint) {
        is TimerHint.NextStep -> stringResource(
            R.string.timer_hint_next_step,
            hint.atMinutes.formatMinutesStudying(),
            hint.breakMinutes.formatMinutesStudying()
        )
        is TimerHint.Earned -> stringResource(
            R.string.timer_hint_earned,
            hint.breakSeconds.formatSecondsToTime()
        )
        null -> return
    }
    Text(
        text = text,
        style = SubBody.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
        textAlign = TextAlign.Center
    )
}
