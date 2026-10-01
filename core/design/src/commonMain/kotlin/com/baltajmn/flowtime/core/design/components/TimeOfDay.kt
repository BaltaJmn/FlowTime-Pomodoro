package com.baltajmn.flowtime.core.design.components

import androidx.compose.runtime.Composable

/** La hora de [millis] como la escribe el sistema: 18:30 o 6:30 p. m., según los ajustes. */
@Composable
expect fun timeOfDay(millis: Long): String
