package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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

    private val holidayId: String = savedStateHandle.get<String>(Routes.ARG_ID).orEmpty()
    private val reloadTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HolidayDetailUiState> = combine(
        reloadTrigger,
        repository.currentUserId
    ) { _, currentUserId ->
        currentUserId
    }
        .flatMapLatest { currentUserId ->
            val parsedId = parseHolidayId(holidayId)

            if (currentUserId == null || parsedId == null) {
                return@flatMapLatest flowOf<HolidayDetailUiState>(HolidayDetailUiState.NotFound)
            }

            flow<HolidayDetailUiState> {
                emit(HolidayDetailUiState.Loading)
                try {
                    val holiday = repository.getHolidayDetails(
                        year = parsedId.year,
                        countryCode = parsedId.countryCode,
                        holidayDate = parsedId.date,
                        name = parsedId.name
                    )

                    if (holiday == null) {
                        emit(HolidayDetailUiState.NotFound)
                        return@flow
                    }

                    repository.addHistoryEntry(currentUserId, holiday)

                    val notesFlow = repository.getNote(currentUserId, holidayId)
                        .map { noteEntity ->
                            HolidayDetailUiState.Success(
                                holiday = holiday.toUi(),
                                noteText = noteEntity?.noteText.orEmpty()
                            )
                        }

                    emitAll(notesFlow)

                } catch (e: Exception) {
                    emit(HolidayDetailUiState.Error(e.localizedMessage ?: "Ошибка загрузки"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HolidayDetailUiState.Loading
        )

    fun loadHoliday() {
        reloadTrigger.value += 1
    }

    fun saveNote(text: String) {
        val currentState = uiState.value
        if (currentState is HolidayDetailUiState.Success) {
            viewModelScope.launch {
                val userId = repository.currentUserId.filterNotNull().first()

                repository.saveNote(
                    userId = userId,
                    holidayId = holidayId,
                    text = text
                )
            }
        }
    }

    private fun parseHolidayId(id: String): ParsedHolidayId? {
        val parts = id.split("|", limit = 3)
        if (parts.size != 3) return null
        val year = runCatching { LocalDate.parse(parts[0]).year }.getOrNull() ?: return null
        return ParsedHolidayId(parts[0], parts[1], parts[2], year)
    }

    private fun PublicHolidayDto.toUi() = HolidayUi(
        id = "$date|$countryCode|$name",
        date = date, localName = localName, name = name,
        countryCode = countryCode, fixed = fixed, global = global, types = types
    )

    private data class ParsedHolidayId(val date: String, val countryCode: String, val name: String, val year: Int)
}