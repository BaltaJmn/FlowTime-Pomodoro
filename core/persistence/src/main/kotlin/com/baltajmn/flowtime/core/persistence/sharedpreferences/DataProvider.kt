package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import java.time.LocalDate

interface DataProvider {
    fun getString(key: SharedPreferencesItem): String?
    fun setString(key: SharedPreferencesItem, value: String)
    fun getBoolean(key: SharedPreferencesItem, defValue: Boolean = true): Boolean
    fun setBoolean(key: SharedPreferencesItem, value: Boolean)
    fun getLong(key: SharedPreferencesItem): Long
    fun setLong(key: SharedPreferencesItem, value: Long)
    fun getFloat(key: String, defValue: Float): Float
    fun setFloat(key: String, value: Float)
    fun setObject(key: SharedPreferencesItem, value: Any)
    fun <T> getObject(key: SharedPreferencesItem, type: Class<T>): T?
    fun getRangeModel(key: SharedPreferencesItem): RangeModel?
    fun getRangeModelList(key: SharedPreferencesItem): MutableList<RangeModel>?
    fun setRangeModel(key: SharedPreferencesItem, value: RangeModel)
    fun updateMinutes(minutes: Long): Long
    fun getMinutesByDate(date: LocalDate): Long
    fun getAllDates(): List<LocalDate>
    fun getStudyTimeMap(): Map<String, Long>
    fun setStudyTimeMap(map: Map<String, Long>)
    fun setCheckValue(key: SharedPreferencesItem, value: Boolean)
    fun getCheckValue(key: SharedPreferencesItem): Boolean
}