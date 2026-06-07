package ru.fefu.publicholidays.ui.model

data class HolidayUi(
    val id: String,
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val fixed: Boolean,
    val global: Boolean,
    val types: List<String>,
    val favoriteNote: String = ""
)