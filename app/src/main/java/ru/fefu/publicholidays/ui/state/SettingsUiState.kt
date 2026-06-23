package ru.fefu.publicholidays.ui.state

import ru.fefu.publicholidays.data.local.UserEntity

data class SettingsUiState(
    val users: List<UserEntity> = emptyList(),
    val currentUserId: String? = null,
    val isDarkTheme: Boolean = false,
    val newUserName: String = "",
    val newUserCountry: String = "RU"
)