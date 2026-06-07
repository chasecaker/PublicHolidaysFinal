package ru.fefu.publicholidays.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_holidays")
data class CachedHolidayEntity(
    @PrimaryKey
    val holidayId: String,
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val year: Int,
    val fixed: Boolean,
    val global: Boolean,
    val cachedAt: Long
)