package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    repository: HolidaysRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = repository.isDarkThemeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}