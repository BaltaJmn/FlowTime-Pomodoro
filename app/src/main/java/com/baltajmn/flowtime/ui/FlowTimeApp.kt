package com.baltajmn.flowtime.ui

import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme

@Composable
fun FlowTimeApp(flowTimeState: FlowTimeState = rememberAppState()) {
    FlowTimeTheme {
        FlowTimeNavHost(
            flowTimeState = flowTimeState
        )
    }
}