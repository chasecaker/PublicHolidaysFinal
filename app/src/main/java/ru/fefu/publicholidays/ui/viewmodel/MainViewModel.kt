package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: HolidaysRepository
) : ViewModel() {

    init {
        checkAndCreateDefaultUser()
    }

    private fun checkAndCreateDefaultUser() {
        viewModelScope.launch {
            repository.initializeDefaultUserIfNeeded()
        }
    }
}