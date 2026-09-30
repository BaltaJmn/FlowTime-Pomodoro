package com.baltajmn.flowtime.ui

import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme
import com.baltajmn.flowtime.goal.Celebration

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appearance: Appearance,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    celebration: Celebration? = null,
    onCelebrationShown: () -> Unit = {}
) {
    FlowTimeTheme(appearance = appearance) {
        celebration?.let { CelebrationDialog(it, onDismiss = onCelebrationShown) }

        FlowTimeNavHost(
            flowTimeAppState = flowTimeAppState,
            showSound = showSound,
            onSoundChange = onSoundChange
        )
    }
}
