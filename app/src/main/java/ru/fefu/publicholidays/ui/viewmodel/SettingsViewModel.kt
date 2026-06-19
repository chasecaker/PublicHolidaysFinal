package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.local.UserEntity
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import ru.fefu.publicholidays.ui.state.SettingsUiState
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: HolidaysRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllUsers(),
                repository.currentUserId,
                repository.isDarkThemeEnabled
            ) { users, currentUserId, isDarkTheme ->
                SettingsUiState(
                    users = users,
                    currentUserId = currentUserId,
                    isDarkTheme = isDarkTheme,
                    newUserName = _uiState.value.newUserName,
                    newUserCountry = _uiState.value.newUserCountry
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(newUserName = name) }
    }

    fun onCountryChange(countryCode: String) {
        _uiState.update { it.copy(newUserCountry = countryCode) }
    }

    fun createUser() {
        val name = _uiState.value.newUserName.trim()
        val country = _uiState.value.newUserCountry.trim().uppercase()

        if (name.isEmpty() || country.length != 2 || !country.all { it.isLetter() }) {
            return
        }

        viewModelScope.launch {
            val newUser = UserEntity(
                userId = UUID.randomUUID().toString(),
                name = name,
                defaultCountryCode = country
            )

            repository.createUser(newUser)
            repository.switchUser(newUser.userId)

        }
    }

    fun selectUser(userId: String) {
        viewModelScope.launch {
            repository.switchUser(userId)
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            repository.deleteUserCompletely(userId)
        }
    }

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            repository.setThemeEnabled(enabled)
        }
    }
}