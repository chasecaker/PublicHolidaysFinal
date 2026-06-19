package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.local.UserEntity
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import java.util.UUID
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
            val users = repository.getAllUsers().first()

            if (users.isEmpty()) {
                val defaultUser = UserEntity(
                    userId = UUID.randomUUID().toString(),
                    name = "Основной профиль",
                    defaultCountryCode = "RU"
                )

                repository.createUser(defaultUser)
                repository.switchUser(defaultUser.userId)
            }
        }
    }
}