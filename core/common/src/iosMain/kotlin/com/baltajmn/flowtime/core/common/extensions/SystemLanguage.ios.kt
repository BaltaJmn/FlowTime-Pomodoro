package com.baltajmn.flowtime.core.common.extensions

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// El primero de la lista, como los recursos de Compose al elegir los textos ("ru-ES" es ruso).
internal actual fun systemLanguage(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String)?.substringBefore('-') ?: "en"
