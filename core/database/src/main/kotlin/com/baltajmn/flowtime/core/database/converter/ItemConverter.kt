package com.baltajmn.flowtime.core.database.converter

import androidx.room.TypeConverter
import com.baltajmn.flowtime.core.database.model.ListItemDb
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ItemConverter {

    @TypeConverter
    fun List<String>.toStringData(): String {
        return this.joinToString(",")
    }

    @TypeConverter
    fun String.toList(): List<String> {
        return this.split(",")
    }

    @TypeConverter
    fun fromListItemDbList(list: List<ListItemDb>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toListItemDbList(data: String): List<ListItemDb> {
        val listType = object : TypeToken<List<ListItemDb>>() {}.type
        return Gson().fromJson(data, listType)
    }
}