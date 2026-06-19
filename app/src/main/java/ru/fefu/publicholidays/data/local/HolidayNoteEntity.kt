package ru.fefu.publicholidays.data.local

import androidx.room.Entity

@Entity(
    tableName = "holiday_notes",
    primaryKeys = [
        "userId",
        "holidayId"
    ]
)
data class HolidayNoteEntity(
    val userId: String,
    val holidayId: String,
    val noteText: String,
    val updatedAt: Long
)