package ru.fefu.publicholidays.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import ru.fefu.publicholidays.ui.model.HolidayUi
import ru.fefu.publicholidays.ui.navigation.Routes
import ru.fefu.publicholidays.ui.state.HolidayDetailUiState
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HolidayDetailViewModel @Inject constructor(
    private val repository: HolidaysRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var uiState: HolidayDetailUiState by mutableStateOf(HolidayDetailUiState.Loading)
        private set

    private val holidayId: String = savedStateHandle.get<String>(Routes.ARG_ID).orEmpty()

    init {
        if (holidayId.isNotBlank()) {
            loadHoliday()
        } else {
            uiState = HolidayDetailUiState.NotFound
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadHoliday() {
        if (holidayId.isBlank()) {
            uiState = HolidayDetailUiState.NotFound
            return
        }

        val parsedId = parseHolidayId(holidayId)
        if (parsedId == null) {
            uiState = HolidayDetailUiState.NotFound
            return
        }

        uiState = HolidayDetailUiState.Loading

        viewModelScope.launch {
            try {
                val holiday = repository.getHolidayDetails(
                    year = parsedId.year,
                    countryCode = parsedId.countryCode,
                    holidayDate = parsedId.date,
                    name = parsedId.name
                )

                if (holiday == null) {
                    uiState = HolidayDetailUiState.NotFound
                    return@launch
                }

                val userId = repository.currentUserId.first()

                if (userId != null) {
                    repository.addHistoryEntry(userId, holiday)
                }

                repository.currentUserId
                    .flatMapLatest { currentUserId ->
                        if (currentUserId != null) {
                            repository.getNote(
                                userId = currentUserId,
                                holidayDate = holiday.date,
                                countryCode = holiday.countryCode
                            )
                        } else {
                            flowOf(null)
                        }
                    }
                    .collect { noteEntity ->
                        uiState = HolidayDetailUiState.Success(
                            holiday = holiday.toUi(),
                            noteText = noteEntity?.noteText.orEmpty()
                        )
                    }
            } catch (e: Exception) {
                uiState = HolidayDetailUiState.Error(
                    e.localizedMessage ?: "Ошибка загрузки праздника"
                )
            }
        }
    }

    fun saveNote(text: String) {
        val currentState = uiState
        if (currentState is HolidayDetailUiState.Success) {
            viewModelScope.launch {
                val userId = repository.currentUserId.first()
                if (userId != null) {
                    repository.saveNote(
                        userId = userId,
                        holidayDate = currentState.holiday.date,
                        countryCode = currentState.holiday.countryCode,
                        text = text
                    )
                }
            }
        }
    }

    private fun parseHolidayId(id: String): ParsedHolidayId? {
        val parts = id.split("|", limit = 3)
        if (parts.size != 3) return null

        val date = parts[0]
        val countryCode = parts[1]
        val name = parts[2]
        val year = runCatching { LocalDate.parse(date).year }.getOrNull() ?: return null

        return ParsedHolidayId(
            date = date,
            countryCode = countryCode,
            name = name,
            year = year
        )
    }

    private fun PublicHolidayDto.toUi() = HolidayUi(
        id = "$date|$countryCode|$name",
        date = date,
        localName = localName,
        name = name,
        countryCode = countryCode,
        fixed = fixed,
        global = global,
        types = types
    )

    private data class ParsedHolidayId(
        val date: String,
        val countryCode: String,
        val name: String,
        val year: Int
    )
}