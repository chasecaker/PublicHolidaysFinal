package ru.fefu.publicholidays.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import ru.fefu.publicholidays.data.local.AppDatabase
import ru.fefu.publicholidays.data.local.CachedHolidayDao
import ru.fefu.publicholidays.data.local.CachedHolidayEntity
import ru.fefu.publicholidays.data.local.UserDao
import ru.fefu.publicholidays.data.local.UserEntity
import ru.fefu.publicholidays.data.local.UserSettingsManager
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.remote.HolidaysApi
import ru.fefu.publicholidays.data.repository.HolidaysRepository

@RunWith(AndroidJUnit4::class)
class HolidaysRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var cachedHolidayDao: CachedHolidayDao
    private lateinit var userDao: UserDao
    private lateinit var repository: HolidaysRepository

    private val mockApi: HolidaysApi = mock(HolidaysApi::class.java)
    private lateinit var userSettingsManager: UserSettingsManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        cachedHolidayDao = database.cachedHolidayDao()
        userDao = database.userDao()
        userSettingsManager = UserSettingsManager(context)

        repository = HolidaysRepository(
            api = mockApi,
            cachedHolidayDao = cachedHolidayDao,
            userDao = userDao,
            favoriteHolidayDao = database.favoriteHolidayDao(),
            holidayHistoryDao = database.holidayHistoryDao(),
            holidayNoteDao = database.holidayNoteDao(),
            userSettingsManager = userSettingsManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testObserveHolidaysReturnsCorrectData() = runBlocking {
        val countryCode = "RU"
        val year = 2026

        val entity = CachedHolidayEntity(
            holidayId = "2026-01-01|$countryCode|New Year",
            date = "2026-01-01",
            localName = "Новый Год",
            name = "New Year",
            countryCode = countryCode,
            year = year,
            fixed = true,
            global = true,
            counties = null,
            launchYear = null,
            types = listOf("Public"),
            cachedAt = System.currentTimeMillis()
        )

        cachedHolidayDao.insertCachedHolidays(listOf(entity))

        val result = repository.observeHolidays(year, countryCode).first()

        assertEquals(1, result.size)
        assertEquals("New Year", result[0].name)
        assertEquals("RU", result[0].countryCode)
    }

    @Test
    fun testFavoritesAndHistoryOperations() = runBlocking {
        val userId = "test_user_id"
        val user = UserEntity(userId, "Тестовый Профиль", "RU")
        repository.createUser(user)

        val holiday = PublicHolidayDto(
            date = "2026-05-01",
            localName = "Праздник Весны и Труда",
            name = "Labour Day",
            countryCode = "RU",
            fixed = true,
            global = true,
            counties = null,
            launchYear = null,
            types = listOf("Public")
        )
        val holidayId = "2026-05-01|RU|Labour Day"

        repository.toggleFavorite(userId, holiday)
        var favoriteIds = repository.getFavoriteIds(userId).first()
        assertTrue(favoriteIds.contains(holidayId))

        repository.toggleFavorite(userId, holiday)
        favoriteIds = repository.getFavoriteIds(userId).first()
        assertFalse(favoriteIds.contains(holidayId))

        repository.addHistoryEntry(userId, holiday)
        val history = repository.getHistory(userId).first()
        assertEquals(1, history.size)

        val firstHistoryItem = history[0]
        val currentHistoryId = "${firstHistoryItem.date}|${firstHistoryItem.countryCode}|${firstHistoryItem.name}"
        assertEquals(holidayId, currentHistoryId)

        repository.saveNote(userId = userId, holidayId = holidayId, text = "Важная заметка")
        val note = repository.getNote(userId, holidayId).first()
        assertNotNull(note)
        assertEquals("Важная заметка", note?.noteText)
    }
}