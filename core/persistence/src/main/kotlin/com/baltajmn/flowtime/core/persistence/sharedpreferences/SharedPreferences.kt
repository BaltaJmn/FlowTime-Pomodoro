package com.baltajmn.flowtime.core.persistence.sharedpreferences

import android.content.Context
import com.baltajmn.flowtime.core.persistence.encrypted.CryptoManager
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class SharedPreferencesProvider(
    context: Context
) : DataProvider {

    private val cryptoManager by lazy { CryptoManager() }
    private val keyMinutes = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())

    companion object {
        const val SHARED_CONFIG = "shared_config"
    }

    private val sharedPreferences by lazy {
        context.getSharedPreferences(SHARED_CONFIG, Context.MODE_PRIVATE)
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

    override fun getBoolean(key: SharedPreferencesItem): Boolean {
        return sharedPreferences.getBoolean(key.name.lowercase(), true)
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
        sharedPreferences.edit().putLong(keyMinutes, getMinutes() + minutes).apply()
        return getMinutes()
    }

    override fun getMinutesByDate(date: LocalDate): Long {
        return sharedPreferences.getLong(
            date.format(
                DateTimeFormatter.ofPattern("ddMMyyyy", Locale.getDefault())
            ),
            0L
        )
    }

    override fun getAllDates(): List<LocalDate> {
        val allKeys = sharedPreferences.all.keys
        val dateFormatter = DateTimeFormatter.ofPattern("ddMMyyyy", Locale.getDefault())
        return allKeys.mapNotNull { key ->
            try {
                LocalDate.parse(key, dateFormatter)
            } catch (e: Exception) {
                null
            }
        }
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

    private fun getMinutes(): Long {
        return sharedPreferences.getLong(keyMinutes, 0L)
    }

}