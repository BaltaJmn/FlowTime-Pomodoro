package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.goal.DailyGoal
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Ajustes › Objetivo diario: los minutos de cada día, de 5 en 5, y la racha. Siempre gratis. */
@Composable
fun GoalCard(goal: GoalUiState, onChange: (delta: Int) -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.goal_title),
                style = LargeTitle.copy(
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            GoalStepper(minutes = goal.minutes, onChange = onChange)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = streakText(goal.streak),
                style = SubBody.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Los minutos de cada día, de 5 en 5. También en la introducción. */
@Composable
fun GoalStepper(minutes: Int, onChange: (delta: Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            text = "−",
            description = stringResource(Res.string.goal_decrease),
            enabled = minutes > DailyGoal.MIN_MINUTES
        ) { onChange(-DailyGoal.STEP_MINUTES) }
        Text(
            text = stringResource(Res.string.goal_per_day, minutes.toLong().formatMinutesStudying()),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = SubBody.copy(fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        )
        StepButton(
            text = "+",
            description = stringResource(Res.string.goal_increase),
            enabled = minutes < DailyGoal.MAX_MINUTES
        ) { onChange(DailyGoal.STEP_MINUTES) }
    }
}

@Composable
private fun StepButton(text: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.semantics { contentDescription = description }
    ) {
        Text(text = text, fontSize = 20.sp)
    }
}

@Composable
internal fun streakText(streak: Streak): String {
    val best = stringResource(Res.string.streak_best, streak.best)
    if (streak.current == 0) {
        val none = stringResource(Res.string.streak_none)
        return if (streak.best > 0) "$none · $best" else none
    }
    val days = pluralStringResource(Res.plurals.streak_days, streak.current, streak.current)
    val text = if (streak.best > streak.current) "$days · $best" else days
    return if (streak.freeDayUsedThisWeek) {
        text + "\n" + stringResource(Res.string.streak_free_day_used)
    } else {
        text
    }
}
