package ru.fefu.publicholidays.ui.state

import ru.fefu.publicholidays.ui.model.HolidayUi

sealed class HolidaysUiState {
    object Loading : HolidaysUiState()

    data class Success(
        val data: List<HolidayUi>,
        val favourites: Set<String>
    ) : HolidaysUiState()

    data class Error(val message: String) : HolidaysUiState()

    object Empty : HolidaysUiState()
}