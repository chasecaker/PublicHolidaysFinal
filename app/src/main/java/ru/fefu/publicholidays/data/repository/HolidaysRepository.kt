package ru.fefu.publicholidays.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import ru.fefu.publicholidays.data.local.CachedHolidayDao
import ru.fefu.publicholidays.data.local.CachedHolidayEntity
import ru.fefu.publicholidays.data.local.FavoriteHolidayDao
import ru.fefu.publicholidays.data.local.FavoriteHolidayEntity
import ru.fefu.publicholidays.data.local.HolidayHistoryDao
import ru.fefu.publicholidays.data.local.HolidayHistoryEntity
import ru.fefu.publicholidays.data.local.HolidayNoteDao
import ru.fefu.publicholidays.data.local.HolidayNoteEntity
import ru.fefu.publicholidays.data.local.UserDao
import ru.fefu.publicholidays.data.local.UserEntity
import ru.fefu.publicholidays.data.local.UserSettingsManager
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.remote.HolidaysApi
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID

@Singleton
class HolidaysRepository @Inject constructor(
    private val api: HolidaysApi,
    private val favoriteHolidayDao: FavoriteHolidayDao,
    private val userDao: UserDao,
    private val holidayHistoryDao: HolidayHistoryDao,
    private val holidayNoteDao: HolidayNoteDao,
    private val userSettingsManager: UserSettingsManager,
    private val cachedHolidayDao: CachedHolidayDao
) {

    companion object {
        private const val HISTORY_LIMIT = 50
        private val CACHE_TTL_MILLIS = TimeUnit.DAYS.toMillis(7)
    }

    val currentUserId: Flow<String?> = userSettingsManager.currentUserId

    val isDarkThemeEnabled: Flow<Boolean> = userSettingsManager.isDarkThemeEnabled

    suspend fun switchUser(userId: String?) {
        userSettingsManager.setCurrentUserId(userId)
    }

    suspend fun setThemeEnabled(enabled: Boolean) {
        userSettingsManager.setDarkThemeEnabled(enabled)
    }

    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()

    suspend fun createUser(user: UserEntity) = userDao.insertUser(user)

    suspend fun deleteUserCompletely(userId: String) {
        favoriteHolidayDao.deleteAllFavoritesByUser(userId)
        holidayHistoryDao.clearHistoryByUser(userId)
        holidayNoteDao.deleteAllNotesByUser(userId)
        userDao.deleteUser(userId)

        if (currentUserId.firstOrNull() == userId) {
            val nextUser = userDao.getAllUsers().firstOrNull()?.firstOrNull()
            switchUser(nextUser?.userId)
        }
    }

    fun observeHolidays(
        year: Int,
        countryCode: String
    ): Flow<List<PublicHolidayDto>> {
        return cachedHolidayDao.observeCachedHolidays(countryCode, year)
            .map { list -> list.map { it.toCachedDto() } }
    }

    suspend fun syncHolidays(
        year: Int,
        countryCode: String,
        forceRefresh: Boolean = false
    ) {
        val cached = cachedHolidayDao.getCachedHolidays(countryCode, year)

        if (!forceRefresh && cached.isNotEmpty() && isCacheFresh(countryCode, year)) {
            return
        }

        try {
            val remote = api.getPublicHolidays(year, countryCode)
            val now = System.currentTimeMillis()
            val entities = remote.map { dto ->
                CachedHolidayEntity(
                    holidayId = dto.toHolidayId(),
                    date = dto.date,
                    localName = dto.localName,
                    name = dto.name,
                    countryCode = dto.countryCode,
                    year = year,
                    fixed = dto.fixed,
                    global = dto.global,
                    counties = dto.counties,
                    launchYear = dto.launchYear,
                    types = dto.types,
                    cachedAt = now
                )
            }

            cachedHolidayDao.clearCache(countryCode, year)
            if (entities.isNotEmpty()) {
                cachedHolidayDao.insertCachedHolidays(entities)
            }

        } catch (e: Exception) {
            if (cached.isEmpty()) {
                throw e
            }
        }
    }

    suspend fun getHolidayDetails(
        year: Int,
        countryCode: String,
        holidayDate: String,
        name: String
    ): PublicHolidayDto? {
        val localHoliday = cachedHolidayDao.getCachedHoliday(countryCode, year, holidayDate, name)

        if (localHoliday == null || !isCacheFresh(countryCode, year)) {
            try {
                syncHolidays(year, countryCode, forceRefresh = false)
            } catch (e: Exception) {
                Log.w("HolidaysRepository", "Не удалось обновить кэш для деталей праздника", e)
                localHoliday?.let { return it.toCachedDto() }
                throw e
            }
        }

        return cachedHolidayDao.getCachedHoliday(countryCode, year, holidayDate, name)?.toCachedDto()
    }

    fun getFavoriteIds(userId: String): Flow<Set<String>> =
        favoriteHolidayDao.getFavoriteIdsByUser(userId).map { it.toSet() }

    fun getFavoriteItems(userId: String): Flow<List<FavoriteHolidayEntity>> =
        favoriteHolidayDao.getFavoritesByUser(userId)

    suspend fun toggleFavorite(userId: String, holiday: PublicHolidayDto) {
        val favoriteId = holiday.toHolidayId()
        val exists = favoriteHolidayDao.isFavoriteByUser(userId, favoriteId)

        if (exists) {
            favoriteHolidayDao.deleteFavoriteByUserAndId(userId, favoriteId)
        } else {
            favoriteHolidayDao.insertFavorite(
                FavoriteHolidayEntity(
                    userId = userId,
                    favoriteId = favoriteId,
                    date = holiday.date,
                    localName = holiday.localName,
                    name = holiday.name,
                    countryCode = holiday.countryCode,
                    fixed = holiday.fixed,
                    global = holiday.global,
                    counties = holiday.counties,
                    launchYear = holiday.launchYear,
                    types = holiday.types,
                    favoriteNote = "",
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun getHistory(userId: String): Flow<List<HolidayHistoryEntity>> =
        holidayHistoryDao.getHistoryByUser(userId, HISTORY_LIMIT)

    suspend fun addHistoryEntry(userId: String, holiday: PublicHolidayDto) {
        holidayHistoryDao.deleteDuplicateHistoryEntry(
            userId = userId, date = holiday.date,
            countryCode = holiday.countryCode, name = holiday.name
        )
        holidayHistoryDao.insertHistoryEntry(
            HolidayHistoryEntity(
                userId = userId, date = holiday.date,
                countryCode = holiday.countryCode, name = holiday.name,
                localName = holiday.localName, timestamp = System.currentTimeMillis()
            )
        )
        holidayHistoryDao.trimHistory(userId, HISTORY_LIMIT)
    }

    suspend fun clearHistory(userId: String) = holidayHistoryDao.clearHistoryByUser(userId)

    fun getNote(userId: String, holidayId: String): Flow<HolidayNoteEntity?> {
        return holidayNoteDao.getNoteByUserAndHoliday(userId, holidayId)
    }

    fun getNotesByUser(userId: String): Flow<List<HolidayNoteEntity>> =
        holidayNoteDao.getNotesByUser(userId)

    suspend fun saveNote(userId: String, holidayId: String, text: String) {
        if (text.isBlank()) {
            holidayNoteDao.deleteNoteByUserAndHoliday(userId, holidayId)
        } else {
            holidayNoteDao.insertNote(
                HolidayNoteEntity(
                    userId = userId,
                    holidayId = holidayId,
                    noteText = text,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun isCacheFresh(countryCode: String, year: Int): Boolean {
        val lastUpdate = cachedHolidayDao.getLastCacheUpdateTime(countryCode, year) ?: return false
        return System.currentTimeMillis() - lastUpdate <= CACHE_TTL_MILLIS
    }

    private fun PublicHolidayDto.toHolidayId(): String = "$date|$countryCode|$name"

    private fun CachedHolidayEntity.toCachedDto() = PublicHolidayDto(
        date = date, localName = localName, name = name,
        countryCode = countryCode, fixed = fixed, global = global,
        counties = counties, launchYear = launchYear, types = types
    )

    suspend fun initializeDefaultUserIfNeeded() {
        val users = userDao.getAllUsers().firstOrNull()?.firstOrNull()
        if (users == null) {
            val defaultUser = UserEntity(
                userId = UUID.randomUUID().toString(),
                name = "Основной профиль",
                defaultCountryCode = "RU"
            )
            userDao.insertUser(defaultUser)
            userSettingsManager.setCurrentUserId(defaultUser.userId)
        }
    }
}