package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

/** Los minutos de hoy, y frente al objetivo diario cuando ya se sabe cuál es. */
@Composable
fun MinutesStudying(minutesStudying: String, goal: String = "") {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (goal.isEmpty()) {
                minutesStudying
            } else {
                stringResource(Res.string.goal_today, minutesStudying, goal)
            },
            style = SubBody.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        )
    }
}