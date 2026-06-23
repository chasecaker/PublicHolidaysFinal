package ru.fefu.publicholidays.ui.state

import ru.fefu.publicholidays.ui.model.HolidayUi

sealed class FavouriteHolidaysUiState {
    object Loading : FavouriteHolidaysUiState()

    data class Success(val data: List<HolidayUi>) : FavouriteHolidaysUiState()

    data class Error(val message: String) : FavouriteHolidaysUiState()

    object Empty : FavouriteHolidaysUiState()
}