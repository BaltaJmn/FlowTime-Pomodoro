package com.baltajmn.flowtime.core.design.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Date

@Composable
actual fun timeOfDay(millis: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(millis))
