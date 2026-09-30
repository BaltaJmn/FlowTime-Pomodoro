package com.baltajmn.flowtime.data.fakes

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem

/** Preferencias en memoria, por el nombre de cada clave. Los objetos pasan por JSON, como en el móvil. */
class FakeDataProvider : DataProvider {

    val values = mutableMapOf<String, Any>()

    override fun getString(key: SharedPreferencesItem) = values[key.name] as? String

    override fun setString(key: SharedPreferencesItem, value: String) {
        values[key.name] = value
    }

    override fun getBoolean(key: SharedPreferencesItem, defValue: Boolean) =
        values[key.name] as? Boolean ?: defValue

    override fun setBoolean(key: SharedPreferencesItem, value: Boolean) {
        values[key.name] = value
    }

    override fun getLong(key: SharedPreferencesItem) = values[key.name] as? Long ?: 0L

    override fun setLong(key: SharedPreferencesItem, value: Long) {
        values[key.name] = value
    }

    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue

    override fun setFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getStudyTimeMap() = values
        .filter { (key, value) -> value is Long && DayKeys.parse(key) != null }
        .mapValues { (_, value) -> value as Long }

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) {
        values[key.name] = value
    }

    override fun getCheckValue(key: SharedPreferencesItem) = values[key.name] as? Boolean ?: true
}
