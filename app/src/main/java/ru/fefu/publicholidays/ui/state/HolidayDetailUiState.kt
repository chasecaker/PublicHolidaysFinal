package ru.fefu.publicholidays.ui.state

import ru.fefu.publicholidays.ui.model.HolidayUi

sealed interface HolidayDetailUiState {
    data object Loading : HolidayDetailUiState
    data class Error(val message: String) : HolidayDetailUiState
    data class Success(
        val holiday: HolidayUi,
        val noteText: String = ""
    ) : HolidayDetailUiState
    data object NotFound : HolidayDetailUiState
}