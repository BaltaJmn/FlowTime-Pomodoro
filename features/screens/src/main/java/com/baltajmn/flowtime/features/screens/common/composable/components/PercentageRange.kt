package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.LargeTitle

@Composable
fun PercentageRange(percentage: Long, onPercentageChange: (Long) -> Unit) {
    var sliderPosition by remember { mutableFloatStateOf(percentage.toFloat()) }

    Column {
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = { onPercentageChange(sliderPosition.toLong()) },
            valueRange = 0f..100f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.tertiary
            )
        )
        Text(
            text = sliderPosition.toInt().toString() + "%",
            style = LargeTitle.copy(fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        )
    }
}