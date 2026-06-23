package ru.fefu.publicholidays.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.repository.HolidaysRepository
import ru.fefu.publicholidays.ui.model.HolidayUi
import ru.fefu.publicholidays.ui.state.HolidaysUiState
import javax.inject.Inject

@HiltViewModel
class HolidaysViewModel @Inject constructor(
    private val repository: HolidaysRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _searchParams = MutableStateFlow(Pair(2026, "RU"))

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _holidays: StateFlow<List<PublicHolidayDto>> = _searchParams
        .flatMapLatest { (year, countryCode) ->
            repository.observeHolidays(year, countryCode)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteIds: StateFlow<Set<String>> = repository.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) repository.getFavoriteIds(userId) else flowOf(emptySet())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val uiState: StateFlow<HolidaysUiState> = combine(
        _holidays,
        _isLoading,
        _errorMessage,
        _query,
        favoriteIds
    ) { holidays, isLoading, error, searchQuery, favIds ->
        when {
            error != null && holidays.isEmpty() -> HolidaysUiState.Error(error)
            isLoading && holidays.isEmpty() -> HolidaysUiState.Loading
            holidays.isEmpty() -> HolidaysUiState.Empty
            else -> {
                val filtered = holidays.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.localName.contains(searchQuery, ignoreCase = true)
                }.map { it.toUi() }

                HolidaysUiState.Success(data = filtered, favourites = favIds)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HolidaysUiState.Loading)

    init {
        viewModelScope.launch {
            repository.currentUserId.collect { userId ->
                if (userId != null) {
                    val allUsers = repository.getAllUsers().first()
                    val currentUser = allUsers.find { it.userId == userId }
                    currentUser?.let {
                        loadHolidays(2026, it.defaultCountryCode)
                    }
                } else {
                    _errorMessage.value = "Ошибка инициализации профиля: пользователь не авторизован"
                }
            }
        }
    }

    fun loadHolidays() {
        val params = _searchParams.value
        loadHolidays(params.first, params.second, forceRefresh = false)
    }

    fun refresh() {
        val params = _searchParams.value
        loadHolidays(params.first, params.second, forceRefresh = true)
    }


    fun loadHolidays(year: Int, countryCode: String, forceRefresh: Boolean = false) {
        _searchParams.value = Pair(year, countryCode)

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.syncHolidays(year, countryCode, forceRefresh)
            } catch (e: Exception) {
                if (_holidays.value.isEmpty()) {
                    _errorMessage.value = e.localizedMessage ?: "Ошибка загрузки данных"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun onSearchQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun toggleFavorite(holidayId: String) {
        viewModelScope.launch {
            val userId = repository.currentUserId.first()
            if (userId == null) {
                _errorMessage.value = "Не удалось определить текущий профиль пользователя"
                return@launch
            }

            val holidayDto = _holidays.value.find {
                "${it.date}|${it.countryCode}|${it.name}" == holidayId
            }
            if (holidayDto == null) {
                _errorMessage.value = "Не удалось найти выбранный праздник в списке"
                return@launch
            }

            try {
                repository.toggleFavorite(userId, holidayDto)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Не удалось обновить избранное"
            }
        }
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
}