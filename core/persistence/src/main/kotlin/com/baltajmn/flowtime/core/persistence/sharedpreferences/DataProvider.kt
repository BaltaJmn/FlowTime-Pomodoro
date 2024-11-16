package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import java.time.LocalDate

interface DataProvider {
    fun getString(key: SharedPreferencesItem, decrypt: Boolean = false): String?
    fun setString(key: SharedPreferencesItem, value: String, encrypt: Boolean = false)
    fun getBoolean(key: SharedPreferencesItem): Boolean
    fun setBoolean(key: SharedPreferencesItem, value: Boolean)
    fun getLong(key: SharedPreferencesItem): Long
    fun setLong(key: SharedPreferencesItem, value: Long)
    fun setObject(key: SharedPreferencesItem, value: Any)
    fun getRangeModel(key: SharedPreferencesItem): RangeModel?
    fun getRangeModelList(key: SharedPreferencesItem): MutableList<RangeModel>?
    fun setRangeModel(key: SharedPreferencesItem, value: RangeModel)
    fun updateMinutes(minutes: Long): Long
    fun getMinutesByDate(date: LocalDate): Long
    fun getAllDates(): List<LocalDate>
    fun setCheckValue(key: SharedPreferencesItem, value: Boolean)
    fun getCheckValue(key: SharedPreferencesItem): Boolean
}