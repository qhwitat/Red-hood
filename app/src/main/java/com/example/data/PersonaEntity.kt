package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_personas")
data class PersonaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val systemInstruction: String,
    val isTemplate: Boolean,
    val iconName: String
)
