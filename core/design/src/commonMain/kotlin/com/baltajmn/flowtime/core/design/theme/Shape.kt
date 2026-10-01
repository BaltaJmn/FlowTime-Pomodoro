package com.baltajmn.flowtime.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Redondeado, como el anillo y los botones. small son los chips: 16 dp los deja en píldora sin
// deformar lo más alto que también la usa, como las cajas de la hora del recordatorio.
val Shapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)
