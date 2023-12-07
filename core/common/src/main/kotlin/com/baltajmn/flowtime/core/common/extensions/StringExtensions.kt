package com.baltajmn.flowtime.core.common.extensions

fun String.capitalizeFirst(): String {
    return this.lowercase().replaceFirstChar(Char::uppercaseChar)
}

fun String.capitalizeWords(): String {
    return this
        .split(' ')
        .joinToString(" ") {
            it.replaceFirstChar(Char::uppercaseChar)
        }
}

fun String.isNumeric(): Boolean = this.matches("-?[0-9]+(\\.[0-9]+)?".toRegex())
fun String.isNumericOrBlank(): Boolean =
    this.matches("-?[0-9]+(\\.[0-9]+)?".toRegex()) || this.isBlank()
