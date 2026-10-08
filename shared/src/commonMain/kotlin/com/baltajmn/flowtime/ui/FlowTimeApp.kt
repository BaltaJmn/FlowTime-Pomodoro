package com.baltajmn.flowtime.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme
import com.baltajmn.flowtime.features.screens.pro.ProRequest
import com.baltajmn.flowtime.features.screens.pro.ProSheet
import com.baltajmn.flowtime.data.goal.Celebration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appearance: Appearance,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    celebration: Celebration? = null,
    onCelebrationShown: () -> Unit = {},
    proRequest: ProRequest? = null,
    onProClosed: () -> Unit = {},
    openFocus: Flow<Unit> = emptyFlow(),
    onAddQuickTile: (() -> Unit)? = null
) {
    FlowTimeTheme(appearance = appearance) {
        proRequest?.let { ProSheet(from = it.from, onDismiss = onProClosed) }

        Box(modifier = Modifier.fillMaxSize()) {
            FlowTimeNavHost(
                flowTimeAppState = flowTimeAppState,
                showSound = showSound,
                onSoundChange = onSoundChange,
                openFocus = openFocus,
                onAddQuickTile = onAddQuickTile
            )
            CelebrationBanner(
                celebration = celebration,
                onDismiss = onCelebrationShown,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
