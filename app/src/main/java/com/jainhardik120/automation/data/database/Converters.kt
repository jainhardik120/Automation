package com.jainhardik120.automation.data.database

import androidx.room.TypeConverter
import com.jainhardik120.automation.utils.KeyAction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return Json.encodeToString(strings)
    }

    @TypeConverter
    fun toStringList(string: String): List<String> {
        return Json.decodeFromString(string)
    }

    @TypeConverter
    fun fromKeyActionList(actions: List<KeyAction>): String {
        return Json.encodeToString(actions)
    }

    @TypeConverter
    fun toKeyActionList(actions: String): List<KeyAction> {
        return Json.decodeFromString(actions)
    }

}