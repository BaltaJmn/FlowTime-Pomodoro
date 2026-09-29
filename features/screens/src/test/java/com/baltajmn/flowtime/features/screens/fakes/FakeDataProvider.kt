package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem

/** Preferencias en memoria. Los objetos se guardan tal cual, sin pasar por JSON. */
class FakeDataProvider : DataProvider {

    val values = mutableMapOf<String, Any>()

    private val SharedPreferencesItem.key get() = name.lowercase()

    override fun getString(key: SharedPreferencesItem) = values[key.key] as? String

    override fun setString(key: SharedPreferencesItem, value: String) {
        values[key.key] = value
    }

    override fun getBoolean(key: SharedPreferencesItem, defValue: Boolean) =
        values[key.key] as? Boolean ?: defValue

    override fun setBoolean(key: SharedPreferencesItem, value: Boolean) {
        values[key.key] = value
    }

    override fun getLong(key: SharedPreferencesItem) = values[key.key] as? Long ?: 0L

    override fun setLong(key: SharedPreferencesItem, value: Long) {
        values[key.key] = value
    }

    override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue

    override fun setFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun setObject(key: SharedPreferencesItem, value: Any) {
        values[key.key] = value
    }

    override fun <T> getObject(key: SharedPreferencesItem, type: Class<T>): T? =
        values[key.key]?.takeIf(type::isInstance)?.let(type::cast)

    override fun getRangeModel(key: SharedPreferencesItem) = values[key.key] as? RangeModel

    @Suppress("UNCHECKED_CAST")
    override fun getRangeModelList(key: SharedPreferencesItem) =
        (values[key.key] as? List<RangeModel>)?.toMutableList()

    override fun setRangeModel(key: SharedPreferencesItem, value: RangeModel) {
        values[key.key] = value
    }

    override fun getStudyTimeMap() = values
        .filter { (key, value) -> value is Long && DayKeys.parse(key) != null }
        .mapValues { (_, value) -> value as Long }

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) {
        values[key.key] = value
    }

    override fun getCheckValue(key: SharedPreferencesItem) = values[key.key] as? Boolean ?: true
}
