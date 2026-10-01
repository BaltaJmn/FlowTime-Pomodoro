package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults

@Composable
fun PercentageRange(percentage: Long, onPercentageChange: (Long) -> Unit) {
    var sliderPosition by remember(percentage) { mutableFloatStateOf(percentage.toFloat()) }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onPercentageChange(sliderPosition.toLong()) },
            // De 5 en 5 y sin el 0: un descanso del 0 % no es un descanso.
            valueRange = TimerDefaults.MIN_PERCENTAGE.toFloat()..TimerDefaults.MAX_PERCENTAGE.toFloat(),
            steps = 18
        )
        Text(
            text = sliderPosition.toInt().toString() + "%",
            style = LargeTitle.copy(fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        )
    }
}