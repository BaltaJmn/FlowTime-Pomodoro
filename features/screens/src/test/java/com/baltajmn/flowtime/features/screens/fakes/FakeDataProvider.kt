package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import java.time.LocalDate

/** Preferencias en memoria. Los objetos se guardan tal cual, sin pasar por JSON. */
class FakeDataProvider(private val today: LocalDate = LocalDate.of(2026, 9, 29)) : DataProvider {

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

    override fun getRangeModel(key: SharedPreferencesItem) = values[key.key] as? RangeModel

    @Suppress("UNCHECKED_CAST")
    override fun getRangeModelList(key: SharedPreferencesItem) =
        (values[key.key] as? List<RangeModel>)?.toMutableList()

    override fun setRangeModel(key: SharedPreferencesItem, value: RangeModel) {
        values[key.key] = value
    }

    override fun updateMinutes(minutes: Long): Long {
        val key = DayKeys.of(today)
        val total = (values[key] as? Long ?: 0L) + minutes
        values[key] = total
        return total
    }

    override fun getMinutesByDate(date: LocalDate) = values[DayKeys.of(date)] as? Long ?: 0L

    override fun getAllDates() = values.keys.mapNotNull(DayKeys::parse)

    override fun getStudyTimeMap() = values
        .filter { (key, value) -> value is Long && DayKeys.parse(key) != null }
        .mapValues { (_, value) -> value as Long }

    override fun setStudyTimeMap(map: Map<String, Long>) {
        values.putAll(map)
    }

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) {
        values[key.key] = value
    }

    override fun getCheckValue(key: SharedPreferencesItem) = values[key.key] as? Boolean ?: true
}
