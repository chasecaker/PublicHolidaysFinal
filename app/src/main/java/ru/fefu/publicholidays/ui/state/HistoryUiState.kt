package ru.fefu.publicholidays.ui.state

import ru.fefu.publicholidays.data.local.HolidayHistoryEntity

data class HistoryUiState(
    val historyItems: List<HolidayHistoryEntity> = emptyList(),
    val isLoading: Boolean = false
)