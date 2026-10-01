package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.app_icon_cherry
import com.baltajmn.flowtime.core.design.resources.app_icon_coral
import com.baltajmn.flowtime.core.design.resources.app_icon_default
import com.baltajmn.flowtime.core.design.resources.app_icon_lavender
import com.baltajmn.flowtime.core.design.resources.app_icon_mint
import com.baltajmn.flowtime.core.design.resources.app_icon_night
import com.baltajmn.flowtime.core.design.resources.app_icon_sand
import org.jetbrains.compose.resources.imageResource
import platform.UIKit.UIApplication
import platform.UIKit.alternateIconName
import platform.UIKit.setAlternateIconName

/**
 * Los iconos alternativos de Assets.xcassets (tools/ios/iconos.py), que el iPhone cambia al momento
 * avisando con su alerta.
 */
actual class AppIcons {
    actual fun current(): AppIcon =
        AppIcon.entries.firstOrNull { it.iconName == UIApplication.sharedApplication.alternateIconName } ?: AppIcon.DEFAULT

    actual fun set(icon: AppIcon) = UIApplication.sharedApplication.setAlternateIconName(icon.iconName, null)
}

/** El juego de iconos en Assets.xcassets; el de siempre es el principal, sin nombre. */
private val AppIcon.iconName: String?
    get() = if (this == AppIcon.DEFAULT) null else "AppIcon-${name.lowercase()}"

internal actual val asksBeforeIconChange = false

@Composable
internal actual fun rememberAppIconImage(icon: AppIcon, px: Int): ImageBitmap = imageResource(
    when (icon) {
        AppIcon.DEFAULT -> Res.drawable.app_icon_default
        AppIcon.LAVENDER -> Res.drawable.app_icon_lavender
        AppIcon.MINT -> Res.drawable.app_icon_mint
        AppIcon.CORAL -> Res.drawable.app_icon_coral
        AppIcon.SAND -> Res.drawable.app_icon_sand
        AppIcon.NIGHT -> Res.drawable.app_icon_night
        AppIcon.CHERRY -> Res.drawable.app_icon_cherry
    }
)
