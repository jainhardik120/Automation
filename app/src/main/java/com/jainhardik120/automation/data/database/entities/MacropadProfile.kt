package com.jainhardik120.automation.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jainhardik120.automation.utils.KeyAction

@Entity
data class MacropadProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val strings: List<String>,
    val actions: List<KeyAction>
)
