package com.baltajmn.flowtime.core.persistence.sharedpreferences

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Claves de los minutos de cada día, con el formato ddMMyyyy: el historial de antes de las
 * sesiones y el texto que se exporta e importa.
 *
 * Siempre con dígitos latinos. Antes se escribían con SimpleDateFormat en el idioma del sistema
 * y se leían con DateTimeFormatter, que no usa los dígitos del idioma: en un móvil con dígitos
 * árabes o persas las dos claves podían no coincidir.
 */
object DayKeys {

    private val formatter = DateTimeFormatter.ofPattern("ddMMyyyy", Locale.ROOT)

    fun of(date: LocalDate): String = date.format(formatter)

    fun parse(key: String): LocalDate? {
        val latinKey = normalize(key) ?: return null
        return try {
            LocalDate.parse(latinKey, formatter)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /** La clave con dígitos latinos, o null si no son ocho dígitos de cualquier sistema. */
    fun normalize(key: String): String? {
        if (key.length != 8 || !key.all { it.isDigit() }) return null
        return key.map { Character.forDigit(Character.digit(it, 10), 10) }.joinToString("")
    }
}
