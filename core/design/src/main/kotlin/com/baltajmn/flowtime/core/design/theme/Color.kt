package com.baltajmn.flowtime.core.design.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    Blue, Pink, Orange, Brown, Olive, Marine, Mint
}

val LightBlue = Color(0xFFabcfe1)
val Blue = Color(0xFF65b2d8)
val DarkBlue = Color(0xFF4293bb)
val SurfaceBlue = Color(0xFF9f9f9f)

val LightPink = Color(0xFFe9b7bb)
val Pink = Color(0xFFe26b74)
val DarkPink = Color(0xFFc7444e)
val SurfacePink = Color(0xFFa7a7a7)

val LightOrange = Color(0xFFd3a28d)
val Orange = Color(0xFFcc744e)
val DarkOrange = Color(0xFFa35d3f)
val SurfaceOrange = Color(0xFF8d8d8d)

val LightBrown = Color(0xFFd6bca2)
val Brown = Color(0xFFc99764)
val DarkBrown = Color(0xFFa7794a)
val SurfaceBrown = Color(0xFF979797)

val LightOlive = Color(0xFF99906f)
val Olive = Color(0xFF807653)
val DarkOlive = Color(0xFF635c46)
val SurfaceOlive = Color(0xFF6a6a6a)

val LightMarine = Color(0xFF3e5a68)
val Marine = Color(0xFF2d4957)
val DarkMarine = Color(0xFF273942)
val SurfaceMarine = Color(0xFF424242)

val LightMint = Color(0xFFfafefa)
val Mint = Color(0xFF98fb98)
val DarkMint = Color(0xFF5ce75c)
val SurfaceMint = Color(0xFFcacaca)

val White = Color(0xFFFFFFFF)

val blurryBrush1 = Brush.verticalGradient(colors = listOf(Mint, White))
val blurryBrush2 = Brush.horizontalGradient(colors = listOf(White, Mint))
val blurryBrush3 = Brush.horizontalGradient(colors = listOf(Mint, White))