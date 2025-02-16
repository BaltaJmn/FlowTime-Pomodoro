package com.baltajmn.flowtime.features.screens.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.baltajmn.flowtime.core.design.components.SplashView
import com.baltajmn.flowtime.core.design.components.collectEvents
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    navigateToMainGraph: () -> Unit,
    navigateToOnBoard: () -> Unit
) {
    collectEvents {
        viewModel.event.collectLatest {
            when (it) {
                is SplashViewModel.Event.NavigateToMainGraph -> navigateToMainGraph()
                is SplashViewModel.Event.NavigateToOnBoard -> navigateToOnBoard()
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.checkHasToShowOnBoard()
    }

    SplashView()
}
