package com.baltajmn.flowtime.core.persistence.sharedpreferences

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

interface DataProvider {
    fun getString(key: SharedPreferencesItem): String?
    fun setString(key: SharedPreferencesItem, value: String)
    fun getBoolean(key: SharedPreferencesItem, defValue: Boolean = true): Boolean
    fun setBoolean(key: SharedPreferencesItem, value: Boolean)
    fun getLong(key: SharedPreferencesItem): Long
    fun setLong(key: SharedPreferencesItem, value: Long)
    fun getFloat(key: String, defValue: Float): Float
    fun setFloat(key: String, value: Float)

    /** Los minutos por día de antes de las sesiones (claves ddMMyyyy). Solo para importarlos una vez. */
    fun getStudyTimeMap(): Map<String, Long>
    fun setCheckValue(key: SharedPreferencesItem, value: Boolean)
    fun getCheckValue(key: SharedPreferencesItem): Boolean
}

/** Un objeto guardado como JSON en [key]; null si no hay o no se entiende. */
inline fun <reified T> DataProvider.getObject(key: SharedPreferencesItem): T? =
    parseJsonOrNull(getString(key), serializer<T>())

inline fun <reified T> DataProvider.setObject(key: SharedPreferencesItem, value: T) =
    setString(key, storedJson.encodeToString(serializer<T>(), value))

/**
 * Los mismos nombres de campo que escribía Gson, así que se lee lo guardado por versiones anteriores.
 * Como Gson, se escriben todos los campos: si un valor por defecto cambia, lo guardado no cambia con él.
 */
@PublishedApi
internal val storedJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Un JSON guardado que ya no se entiende (corrupto, o de otra versión) se lee como si no estuviera, y
 * valen los valores por defecto. Si lanzara, la app se cerraría en cada arranque sin forma de salir (#24).
 */
@PublishedApi
internal fun <T> parseJsonOrNull(raw: String?, serializer: KSerializer<T>): T? {
    raw ?: return null
    return try {
        storedJson.decodeFromString(serializer, raw)
    } catch (e: IllegalArgumentException) {
        null
    }
}
