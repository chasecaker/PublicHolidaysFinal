package ru.fefu.publicholidays.data.local

import androidx.room.Entity

@Entity(
    tableName = "holiday_notes",
    primaryKeys = [
        "userId",
        "holidayDate",
        "countryCode"
    ]
)
data class HolidayNoteEntity(
    val userId: String,
    val holidayDate: String,
    val countryCode: String,
    val noteText: String,
    val updatedAt: Long
)