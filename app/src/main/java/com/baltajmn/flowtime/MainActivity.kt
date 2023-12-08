package com.baltajmn.flowtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.ui.FlowTimeApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
    private val theme: MutableState<AppTheme> = mutableStateOf(AppTheme.Blue)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        theme.value = viewModel.getAppTheme()

        setContent {
            FlowTimeApp(
                appTheme = theme.value
            ) { it: AppTheme -> theme.value = it }
        }
    }
}