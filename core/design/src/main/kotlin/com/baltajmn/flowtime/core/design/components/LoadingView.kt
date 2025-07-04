package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R

@Composable
fun LoadingView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
    ) {
        LottieImage(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.Center),
            animation = R.raw.loading,
            tintColor = MaterialTheme.colorScheme.primary
        )
    }
}