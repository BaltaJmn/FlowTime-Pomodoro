package com.baltajmn.flowtime.core.persistence.sharedpreferences

import kotlinx.datetime.LocalDate

/**
 * Claves de los minutos de cada día, con el formato ddMMyyyy: el historial de antes de las
 * sesiones y el texto que se exporta e importa.
 *
 * Siempre con dígitos latinos. Antes se escribían con SimpleDateFormat en el idioma del sistema
 * y se leían con DateTimeFormatter, que no usa los dígitos del idioma: en un móvil con dígitos
 * árabes o persas las dos claves podían no coincidir.
 */
object DayKeys {

    private val format = LocalDate.Format {
        day()
        monthNumber()
        year()
    }

    fun of(date: LocalDate): String = format.format(date)

    fun parse(key: String): LocalDate? = normalize(key)?.let(format::parseOrNull)

    /** La clave con dígitos latinos, o null si no son ocho dígitos de cualquier sistema. */
    fun normalize(key: String): String? {
        if (key.length != 8 || !key.all { it.isDigit() }) return null
        return key.map { it.digitToInt() }.joinToString("")
    }
}
