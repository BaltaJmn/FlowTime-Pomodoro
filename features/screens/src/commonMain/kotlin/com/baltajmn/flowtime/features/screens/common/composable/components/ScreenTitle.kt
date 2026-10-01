package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.LargeTitle

@Composable
fun ScreenTitle(text: String) {
    Text(
        text = text,
        style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
    )
}