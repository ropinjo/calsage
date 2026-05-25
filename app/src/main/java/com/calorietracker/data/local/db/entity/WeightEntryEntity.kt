package com.calorietracker.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Float,

    @ColumnInfo(name = "note")
    val note: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)
