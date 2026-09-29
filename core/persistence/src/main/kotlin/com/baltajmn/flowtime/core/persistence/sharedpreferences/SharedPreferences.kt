package com.baltajmn.flowtime.core.persistence.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import com.baltajmn.flowtime.core.persistence.encrypted.CryptoManager
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.Clock
import java.time.LocalDate

class SharedPreferencesProvider(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone()
) : DataProvider {

    private val cryptoManager by lazy { CryptoManager() }

    companion object {
        const val SHARED_CONFIG = "shared_config"
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(SHARED_CONFIG, Context.MODE_PRIVATE)
            .also(::migrateNonLatinDayKeys)
    }

    override fun getString(key: SharedPreferencesItem, decrypt: Boolean): String? {
        if (decrypt) {
            val encryptedValue =
                sharedPreferences.getString(key.name.lowercase(), null) ?: return null
            return cryptoManager.decrypt(encryptedValue)
        }
        return sharedPreferences.getString(key.name.lowercase(), null)
    }

    override fun setString(key: SharedPreferencesItem, value: String, encrypt: Boolean) {
        val finalValue = if (encrypt) cryptoManager.encrypt(value) else value
        sharedPreferences.edit().putString(key.name.lowercase(), finalValue).apply()
    }

    override fun getBoolean(key: SharedPreferencesItem, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(key.name.lowercase(), defValue)
    }

    override fun setBoolean(key: SharedPreferencesItem, value: Boolean) {
        sharedPreferences.edit().putBoolean(key.name.lowercase(), value).apply()
    }

    override fun getLong(key: SharedPreferencesItem): Long {
        return sharedPreferences.getLong(key.name.lowercase(), 0L)
    }

    override fun setLong(key: SharedPreferencesItem, value: Long) {
        sharedPreferences.edit().putLong(key.name.lowercase(), value).apply()
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return sharedPreferences.getFloat(key, defValue)
    }

    override fun setFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    override fun setObject(key: SharedPreferencesItem, value: Any) {
        val rawString = Gson().toJson(value)
        sharedPreferences.edit().putString(key.name.lowercase(), rawString).apply()
    }

    override fun getRangeModel(key: SharedPreferencesItem): RangeModel? {
        val rawString = sharedPreferences.getString(key.name.lowercase(), null) ?: return null
        return Gson().fromJson(rawString, RangeModel::class.java)
    }

    override fun getRangeModelList(key: SharedPreferencesItem): MutableList<RangeModel>? {
        val rawString = sharedPreferences.getString(key.name.lowercase(), null) ?: return null
        val type: Type = object : TypeToken<MutableList<RangeModel>>() {}.type
        return Gson().fromJson(rawString, type)
    }

    override fun setRangeModel(key: SharedPreferencesItem, value: RangeModel) {
        val rawString = Gson().toJson(value)
        sharedPreferences.edit().putString(key.name.lowercase(), rawString).apply()
    }

    override fun updateMinutes(minutes: Long): Long {
        val todayKey = DayKeys.today(clock)
        val total = sharedPreferences.getLong(todayKey, 0L) + minutes
        sharedPreferences.edit().putLong(todayKey, total).apply()
        return total
    }

    override fun getMinutesByDate(date: LocalDate): Long {
        return sharedPreferences.getLong(DayKeys.of(date), 0L)
    }

    override fun getAllDates(): List<LocalDate> {
        return sharedPreferences.all.keys.mapNotNull(DayKeys::parse)
    }

    override fun getStudyTimeMap(): Map<String, Long> {
        return sharedPreferences.all
            .filter { (key, value) -> value is Long && DayKeys.parse(key) != null }
            .mapValues { (_, value) -> value as Long }
    }

    override fun setStudyTimeMap(map: Map<String, Long>) {
        val editor = sharedPreferences.edit()
        map.forEach { (key, value) ->
            editor.putLong(key, value)
        }
        editor.apply()
    }

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) {
        sharedPreferences.edit()
            .putBoolean(key.name.lowercase(), value)
            .apply()
    }

    override fun getCheckValue(key: SharedPreferencesItem): Boolean {
        return sharedPreferences.getBoolean(
            key.name.lowercase(),
            true
        )
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