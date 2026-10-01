package com.baltajmn.flowtime.core.design

import android.annotation.SuppressLint
import android.content.Context
import org.jetbrains.compose.resources.StringResource

/**
 * Un texto de Compose Multiplatform desde código de Android sin Compose (notificaciones, widget).
 * composeResources es también un directorio de recursos de Android: el texto está en R con su nombre.
 */
@SuppressLint("DiscouragedApi")
fun Context.getString(resource: StringResource): String =
    getString(resources.getIdentifier(resource.key, "string", packageName))
