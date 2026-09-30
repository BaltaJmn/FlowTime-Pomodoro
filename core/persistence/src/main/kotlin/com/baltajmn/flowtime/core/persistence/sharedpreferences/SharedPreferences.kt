package com.baltajmn.flowtime.core.persistence.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class SharedPreferencesProvider(context: Context) : DataProvider {

    companion object {
        const val SHARED_CONFIG = "shared_config"

        /** La sesión en curso, aparte: la copia de seguridad de Android no debe llevársela a otro móvil. */
        const val SESSION_STATE = "session_state"
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(SHARED_CONFIG, Context.MODE_PRIVATE)
            .also(::migrateNonLatinDayKeys)
    }

    // Sus anclas (el reloj desde el arranque y el número de arranque) solo valen en el móvil donde se
    // crearon: restaurada en otro, la sesión contaría un tiempo que no es.
    private val sessionPreferences by lazy {
        context.getSharedPreferences(SESSION_STATE, Context.MODE_PRIVATE)
            .also(::moveSessionState)
    }

    private fun prefs(key: SharedPreferencesItem) =
        if (key == SharedPreferencesItem.TIMER_SESSION) sessionPreferences else sharedPreferences

    override fun getString(key: SharedPreferencesItem): String? {
        return readOrDefault(null) { prefs(key).getString(key.name.lowercase(), null) }
    }

    override fun setString(key: SharedPreferencesItem, value: String) {
        prefs(key).edit().putString(key.name.lowercase(), value).apply()
    }

    override fun getBoolean(key: SharedPreferencesItem, defValue: Boolean): Boolean {
        return readOrDefault(defValue) { sharedPreferences.getBoolean(key.name.lowercase(), defValue) }
    }

    override fun setBoolean(key: SharedPreferencesItem, value: Boolean) {
        sharedPreferences.edit().putBoolean(key.name.lowercase(), value).apply()
    }

    override fun getLong(key: SharedPreferencesItem): Long {
        return readOrDefault(0L) { sharedPreferences.getLong(key.name.lowercase(), 0L) }
    }

    override fun setLong(key: SharedPreferencesItem, value: Long) {
        sharedPreferences.edit().putLong(key.name.lowercase(), value).apply()
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return readOrDefault(defValue) { sharedPreferences.getFloat(key, defValue) }
    }

    override fun setFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    override fun setObject(key: SharedPreferencesItem, value: Any) {
        val rawString = Gson().toJson(value)
        prefs(key).edit().putString(key.name.lowercase(), rawString).apply()
    }

    override fun <T> getObject(key: SharedPreferencesItem, type: Class<T>): T? =
        parseJsonOrNull(getString(key), type)

    override fun getRangeModel(key: SharedPreferencesItem): RangeModel? =
        parseJsonOrNull(getString(key), RangeModel::class.java)

    override fun getRangeModelList(key: SharedPreferencesItem): MutableList<RangeModel>? =
        parseJsonOrNull(getString(key), object : TypeToken<MutableList<RangeModel>>() {}.type)

    override fun setRangeModel(key: SharedPreferencesItem, value: RangeModel) {
        val rawString = Gson().toJson(value)
        sharedPreferences.edit().putString(key.name.lowercase(), rawString).apply()
    }

    override fun getStudyTimeMap(): Map<String, Long> {
        return sharedPreferences.all
            .filter { (key, value) -> value is Long && DayKeys.parse(key) != null }
            .mapValues { (_, value) -> value as Long }
    }

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(key.name.lowercase(), value)
            .apply()
    }

    override fun getCheckValue(key: SharedPreferencesItem): Boolean {
        return readOrDefault(true) { sharedPreferences.getBoolean(key.name.lowercase(), true) }
    }

    /** Un ajuste guardado con otro tipo cerraba la app al leerlo: mejor el valor por defecto. */
    private inline fun <T> readOrDefault(default: T, read: () -> T): T = try {
        read()
    } catch (e: ClassCastException) {
        default
    }

    /** La sesión en curso vivía con los ajustes: se mueve una vez a su fichero. */
    private fun moveSessionState(sessionPrefs: SharedPreferences) {
        val key = SharedPreferencesItem.TIMER_SESSION.name.lowercase()
        val old = sharedPreferences.getString(key, null) ?: return
        if (!sessionPrefs.contains(key)) sessionPrefs.edit().putString(key, old).apply()
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Pasa a dígitos latinos las claves de días guardadas con los dígitos del idioma del sistema,
     * sumando los minutos si ya existía la clave latina.
     */
    private fun migrateNonLatinDayKeys(prefs: SharedPreferences) {
        val minutesByLatinKey = prefs.all
            .filter { (key, value) -> value is Long && DayKeys.normalize(key).let { it != null && it != key } }
            .entries
            .groupBy { DayKeys.normalize(it.key)!! }
        if (minutesByLatinKey.isEmpty()) return

        val editor = prefs.edit()
        minutesByLatinKey.forEach { (latinKey, entries) ->
            val migrated = entries.sumOf { it.value as Long }
            editor.putLong(latinKey, prefs.getLong(latinKey, 0L) + migrated)
            entries.forEach { editor.remove(it.key) }
        }
        editor.apply()
    }
}

/**
 * Un JSON guardado que ya no se entiende (corrupto, o de otra versión) se lee como si no estuviera, y
 * valen los valores por defecto. Si lanzara, la app se cerraría en cada arranque sin forma de salir (#24).
 */
internal fun <T> parseJsonOrNull(raw: String?, type: Type): T? {
    raw ?: return null
    return try {
        Gson().fromJson<T>(raw, type)
    } catch (e: JsonParseException) {
        null
    }
}
