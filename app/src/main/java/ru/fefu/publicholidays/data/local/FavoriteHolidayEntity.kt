package ru.fefu.publicholidays.data.local

import androidx.room.Entity

@Entity(
    tableName = "favorite_holidays",
    primaryKeys = ["userId", "favoriteId"]
)
data class FavoriteHolidayEntity(
    val userId: String,
    val favoriteId: String,
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val fixed: Boolean,
    val global: Boolean,
    val favoriteNote: String = "",
    val addedAt: Long = System.currentTimeMillis()
)