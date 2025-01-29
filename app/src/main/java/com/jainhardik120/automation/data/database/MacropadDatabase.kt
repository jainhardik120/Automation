package com.jainhardik120.automation.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jainhardik120.automation.data.database.entities.MacropadProfile

@Database(entities = [MacropadProfile::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class MacropadDatabase : RoomDatabase() {
    abstract val dao: MacropadDao
}