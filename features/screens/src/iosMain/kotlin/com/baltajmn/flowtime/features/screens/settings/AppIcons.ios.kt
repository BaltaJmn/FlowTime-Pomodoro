package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// ponytail: sin iconos alternativos en el iPhone (hasAppIcons a false). Cuando se quieran:
// UIApplication.setAlternateIconName y los iconos en el Info.plist de iosApp.
actual class AppIcons {
    actual fun current(): AppIcon = AppIcon.DEFAULT

    actual fun set(icon: AppIcon) = Unit
}

@Composable
internal actual fun rememberAppIconImage(icon: AppIcon, px: Int): ImageBitmap = ImageBitmap(px, px)
