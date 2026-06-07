package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.local.FavoriteHolidayEntity
import ru.fefu.publicholidays.data.local.HolidayNoteEntity
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import ru.fefu.publicholidays.ui.model.HolidayUi
import ru.fefu.publicholidays.ui.state.FavouriteHolidaysUiState
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: HolidaysRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FavouriteHolidaysUiState> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                combine(
                    repository.getFavoriteItems(userId),
                    repository.getNotesByUser(userId)
                ) { favorites, notes ->
                    if (favorites.isEmpty()) {
                        FavouriteHolidaysUiState.Empty
                    } else {
                        FavouriteHolidaysUiState.Success(
                            favorites.map { favorite ->
                                favorite.toUi(notes)
                            }
                        )
                    }
                }
            } else {
                flowOf(FavouriteHolidaysUiState.Empty)
            }
        }
        .catch { e ->
            emit(FavouriteHolidaysUiState.Error(e.localizedMessage ?: "Ошибка загрузки избранного"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavouriteHolidaysUiState.Loading
        )

    fun removeFromFavorites(holiday: HolidayUi) {
        viewModelScope.launch {
            val userId = repository.currentUserId.first() ?: return@launch

            repository.toggleFavorite(
                userId = userId,
                holiday = holiday.toDto()
            )
        }
    }

    fun updateFavoriteNote(
        holiday: HolidayUi,
        note: String
    ) {
        viewModelScope.launch {
            val userId = repository.currentUserId.first() ?: return@launch

            repository.saveNote(
                userId = userId,
                holidayDate = holiday.date,
                countryCode = holiday.countryCode,
                text = note
            )
        }
    }

    private fun FavoriteHolidayEntity.toUi(
        notes: List<HolidayNoteEntity>
    ): HolidayUi {
        val linkedNote = notes.firstOrNull { note ->
            note.holidayDate == date && note.countryCode == countryCode
        }

        return HolidayUi(
            id = favoriteId,
            date = date,
            localName = localName,
            name = name,
            countryCode = countryCode,
            fixed = fixed,
            global = global,
            types = emptyList(),
            favoriteNote = linkedNote?.noteText.orEmpty()
        )
    }

    private fun HolidayUi.toDto() = ru.fefu.publicholidays.data.model.PublicHolidayDto(
        date = date,
        localName = localName,
        name = name,
        countryCode = countryCode,
        fixed = fixed,
        global = global,
        counties = null,
        launchYear = null,
        types = types
    )
}