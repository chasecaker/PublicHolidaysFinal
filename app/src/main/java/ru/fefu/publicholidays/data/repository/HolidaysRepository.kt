package ru.fefu.publicholidays.data.repository

import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.first

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

    suspend fun createUser(user: UserEntity) =
        userDao.insertUser(user)

    suspend fun deleteUserCompletely(userId: String) {
        favoriteHolidayDao.deleteAllFavoritesByUser(userId)
        holidayHistoryDao.clearHistoryByUser(userId)
        holidayNoteDao.deleteAllNotesByUser(userId)

        userDao.deleteUser(userId)

        if (currentUserId.first() == userId) {
            val nextUser = userDao.getAllUsers().first().firstOrNull()

            switchUser(nextUser?.userId)
        }
    }

    suspend fun getHolidays(
        year: Int,
        countryCode: String,
        forceRefresh: Boolean = false
    ): List<PublicHolidayDto> {
        val cached = cachedHolidayDao.getCachedHolidays(countryCode, year)

        if (!forceRefresh && cached.isNotEmpty() && isCacheFresh(countryCode, year)) {
            return cached.map { it.toCachedDto() }
        }

        return try {
            val remote = api.getPublicHolidays(year, countryCode)

            if (remote.isNotEmpty()) {
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
                        cachedAt = now
                    )
                }

                cachedHolidayDao.clearCache(countryCode, year)
                cachedHolidayDao.insertCachedHolidays(entities)
            }

            remote
        } catch (e: Exception) {
            if (cached.isNotEmpty()) {
                cached.map { it.toCachedDto() }
            } else {
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
        val cached = cachedHolidayDao.getCachedHoliday(
            countryCode = countryCode,
            year = year,
            holidayDate = holidayDate,
            name = name
        )

        if (cached != null) {
            return cached.toCachedDto()
        }

        return getHolidays(
            year = year,
            countryCode = countryCode,
            forceRefresh = false
        ).find { holiday ->
            holiday.date == holidayDate && holiday.name == name
        }
    }

    fun getFavorites(userId: String): Flow<List<PublicHolidayDto>> {
        return favoriteHolidayDao.getFavoritesByUser(userId)
            .map { list -> list.map { it.toFavoriteDto() } }
    }

    fun getFavoriteIds(userId: String): Flow<Set<String>> {
        return favoriteHolidayDao.getFavoriteIdsByUser(userId)
            .map { ids -> ids.toSet() }
    }

    fun getFavoriteItems(userId: String): Flow<List<FavoriteHolidayEntity>> {
        return favoriteHolidayDao.getFavoritesByUser(userId)
    }

    suspend fun toggleFavorite(
        userId: String,
        holiday: PublicHolidayDto
    ) {
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
                    favoriteNote = "",
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun getHistory(userId: String): Flow<List<HolidayHistoryEntity>> {
        return holidayHistoryDao.getHistoryByUser(
            userId = userId,
            limit = HISTORY_LIMIT
        )
    }

    suspend fun addHistoryEntry(
        userId: String,
        holiday: PublicHolidayDto
    ) {
        holidayHistoryDao.deleteDuplicateHistoryEntry(
            userId = userId,
            date = holiday.date,
            countryCode = holiday.countryCode,
            name = holiday.name
        )

        holidayHistoryDao.insertHistoryEntry(
            HolidayHistoryEntity(
                userId = userId,
                date = holiday.date,
                countryCode = holiday.countryCode,
                name = holiday.name,
                localName = holiday.localName,
                timestamp = System.currentTimeMillis()
            )
        )

        holidayHistoryDao.trimHistory(
            userId = userId,
            limit = HISTORY_LIMIT
        )
    }

    suspend fun clearHistory(userId: String) {
        holidayHistoryDao.clearHistoryByUser(userId)
    }

    fun getNote(
        userId: String,
        holidayDate: String,
        countryCode: String
    ): Flow<HolidayNoteEntity?> {
        return holidayNoteDao.getNoteByUserAndHoliday(
            userId = userId,
            holidayDate = holidayDate,
            countryCode = countryCode
        )
    }

    fun getNotesByUser(userId: String): Flow<List<HolidayNoteEntity>> {
        return holidayNoteDao.getNotesByUser(userId)
    }

    suspend fun saveNote(
        userId: String,
        holidayDate: String,
        countryCode: String,
        text: String
    ) {
        if (text.isBlank()) {
            holidayNoteDao.deleteNoteByUserAndHoliday(
                userId = userId,
                holidayDate = holidayDate,
                countryCode = countryCode
            )
        } else {
            holidayNoteDao.insertNote(
                HolidayNoteEntity(
                    userId = userId,
                    holidayDate = holidayDate,
                    countryCode = countryCode,
                    noteText = text,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun isCacheFresh(
        countryCode: String,
        year: Int
    ): Boolean {
        val lastUpdate = cachedHolidayDao.getLastCacheUpdateTime(
            countryCode = countryCode,
            year = year
        ) ?: return false

        return System.currentTimeMillis() - lastUpdate <= CACHE_TTL_MILLIS
    }

    private fun PublicHolidayDto.toHolidayId(): String {
        return "$date|$countryCode|$name"
    }

    private fun CachedHolidayEntity.toCachedDto() = PublicHolidayDto(
        date = date,
        localName = localName,
        name = name,
        countryCode = countryCode,
        fixed = fixed,
        global = global,
        counties = null,
        launchYear = null,
        types = emptyList()
    )

    private fun FavoriteHolidayEntity.toFavoriteDto() = PublicHolidayDto(
        date = date,
        localName = localName,
        name = name,
        countryCode = countryCode,
        fixed = fixed,
        global = global,
        counties = null,
        launchYear = null,
        types = emptyList()
    )
}