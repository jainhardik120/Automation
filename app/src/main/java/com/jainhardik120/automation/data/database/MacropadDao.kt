package com.jainhardik120.automation.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.jainhardik120.automation.data.database.entities.MacropadProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface MacropadDao {
    @Query("SELECT * FROM MacropadProfile")
    fun getAllProfiles(): Flow<List<MacropadProfile>>

    @Query("SELECT * FROM MacropadProfile WHERE id = :id")
    suspend fun getProfileById(id: Int): MacropadProfile

    @Query("DELETE FROM MacropadProfile WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Upsert
    suspend fun upsertProfile(profile: MacropadProfile)
}