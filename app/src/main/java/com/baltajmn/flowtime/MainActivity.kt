package com.baltajmn.flowtime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.ui.FlowTimeApp

class MainActivity : ComponentActivity() {

    private val theme: MutableState<AppTheme> = mutableStateOf(AppTheme.Blue)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            FlowTimeApp(
                appTheme = theme.value,
            )
        }
    }
}