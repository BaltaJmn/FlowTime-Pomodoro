package com.baltajmn.flowtime.core.common.extensions

import java.util.Locale

internal actual fun systemLanguage(): String = Locale.getDefault().language
