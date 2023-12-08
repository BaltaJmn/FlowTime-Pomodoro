package com.baltajmn.flowtime.ui

import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
) {
    FlowTimeTheme(appTheme = appTheme) {
        FlowTimeNavHost(
            flowTimeAppState = flowTimeAppState,
            onThemeChanged = onThemeChanged,
        )
    }
}