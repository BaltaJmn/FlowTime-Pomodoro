package com.baltajmn.flowtime.core.design.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import com.baltajmn.flowtime.core.design.resources.*

// Composable: en Compose Multiplatform las fuentes de los recursos se cargan dentro de la composición.
val LargeTitle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W900,
        fontSize = 27.sp
    )

val Title: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W700,
        fontSize = 24.sp
    )

val SmallTitle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp
    )

val Body: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )

val SubBody: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W300,
        fontSize = 15.sp
    )

val Button: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Poppins,
        fontWeight = FontWeight.W700,
        fontSize = 18.sp
    )

val FontFamily.Companion.Poppins: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.poppins_light, FontWeight.W300),
        Font(Res.font.poppins_regular, FontWeight.W400),
        Font(Res.font.poppins_bold, FontWeight.W700),
        Font(Res.font.poppins_black, FontWeight.W900)
    )
