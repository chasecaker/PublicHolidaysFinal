package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import ru.fefu.publicholidays.ui.state.HistoryUiState
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HolidaysRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        observeHistory()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeHistory() {
        viewModelScope.launch {
            repository.currentUserId
                .flatMapLatest { userId ->
                    if (userId != null) {
                        repository.getHistory(userId).map { items ->
                            HistoryUiState(historyItems = items, isLoading = false)
                        }
                    } else {
                        flowOf(HistoryUiState(isLoading = false))
                    }
                }
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val userId = repository.currentUserId.first()
            if (userId != null) {
                repository.clearHistory(userId)
            }
        }
    }
}