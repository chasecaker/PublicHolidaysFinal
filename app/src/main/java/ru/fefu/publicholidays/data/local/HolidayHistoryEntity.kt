package ru.fefu.publicholidays.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holiday_history")
data class HolidayHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: String,
    val countryCode: String,
    val name: String,
    val localName: String,
    val timestamp: Long
)