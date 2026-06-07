package ru.fefu.publicholidays.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.fefu.publicholidays.data.local.AppDatabase
import ru.fefu.publicholidays.data.local.CachedHolidayEntity
import ru.fefu.publicholidays.data.local.UserSettingsManager
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.remote.HolidaysApi
import ru.fefu.publicholidays.data.repository.HolidaysRepository

@RunWith(AndroidJUnit4::class)
class HolidaysRepositoryRoomTest {

    private lateinit var database: AppDatabase
    private lateinit var api: FakeHolidaysApi
    private lateinit var repository: HolidaysRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        api = FakeHolidaysApi()

        repository = HolidaysRepository(
            api = api,
            favoriteHolidayDao = database.favoriteHolidayDao(),
            userDao = database.userDao(),
            holidayHistoryDao = database.holidayHistoryDao(),
            holidayNoteDao = database.holidayNoteDao(),
            userSettingsManager = UserSettingsManager(context),
            cachedHolidayDao = database.cachedHolidayDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getHolidays_whenCacheExistsAndNetworkFails_returnsCachedData() = runBlocking {
        database.cachedHolidayDao().insertCachedHolidays(
            listOf(
                CachedHolidayEntity(
                    holidayId = "2026-01-01|RU|New Year's Day",
                    date = "2026-01-01",
                    localName = "Новый год",
                    name = "New Year's Day",
                    countryCode = "RU",
                    year = 2026,
                    fixed = true,
                    global = true,
                    cachedAt = System.currentTimeMillis()
                )
            )
        )

        api.exception = RuntimeException("Network unavailable")

        val result = repository.getHolidays(
            year = 2026,
            countryCode = "RU",
            forceRefresh = true
        )

        assertEquals(1, result.size)
        assertEquals("New Year's Day", result.first().name)
        assertEquals("RU", result.first().countryCode)
    }

    @Test
    fun toggleFavorite_addsAndRemovesFavoriteForSpecificUser() = runBlocking {
        val holiday = newYearHoliday()

        repository.toggleFavorite(
            userId = "user-1",
            holiday = holiday
        )

        var favorites = repository.getFavoriteItems("user-1").first()

        assertEquals(1, favorites.size)
        assertEquals("2026-01-01|RU|New Year's Day", favorites.first().favoriteId)

        repository.toggleFavorite(
            userId = "user-1",
            holiday = holiday
        )

        favorites = repository.getFavoriteItems("user-1").first()

        assertTrue(favorites.isEmpty())
    }

    @Test
    fun saveNote_updatesOnlySelectedUserHolidayNote() = runBlocking {
        val holiday = newYearHoliday()

        repository.toggleFavorite("user-1", holiday)
        repository.toggleFavorite("user-2", holiday)

        repository.saveNote(
            userId = "user-1",
            holidayDate = holiday.date,
            countryCode = holiday.countryCode,
            text = "Поздравить семью"
        )

        val user1Note = repository.getNote(
            userId = "user-1",
            holidayDate = holiday.date,
            countryCode = holiday.countryCode
        ).first()

        val user2Note = repository.getNote(
            userId = "user-2",
            holidayDate = holiday.date,
            countryCode = holiday.countryCode
        ).first()

        assertEquals("Поздравить семью", user1Note?.noteText)
        assertEquals(null, user2Note)
    }

    @Test
    fun addHistoryEntry_whenSameHolidayOpenedTwice_keepsSingleRecentEntry() = runBlocking {
        val holiday = newYearHoliday()

        repository.addHistoryEntry("user-1", holiday)
        repository.addHistoryEntry("user-1", holiday)

        val history = repository.getHistory("user-1").first()

        assertEquals(1, history.size)
        assertEquals("New Year's Day", history.first().name)
    }

    @Test
    fun saveNote_whenTextIsBlank_deletesExistingNote() = runBlocking {
        repository.saveNote(
            userId = "user-1",
            holidayDate = "2026-01-01",
            countryCode = "RU",
            text = "Купить подарок"
        )

        var note = repository.getNote(
            userId = "user-1",
            holidayDate = "2026-01-01",
            countryCode = "RU"
        ).first()

        assertEquals("Купить подарок", note?.noteText)

        repository.saveNote(
            userId = "user-1",
            holidayDate = "2026-01-01",
            countryCode = "RU",
            text = ""
        )

        note = repository.getNote(
            userId = "user-1",
            holidayDate = "2026-01-01",
            countryCode = "RU"
        ).first()

        assertEquals(null, note)
    }

    private fun newYearHoliday() = PublicHolidayDto(
        date = "2026-01-01",
        localName = "Новый год",
        name = "New Year's Day",
        countryCode = "RU",
        fixed = true,
        global = true,
        counties = null,
        launchYear = null,
        types = listOf("Public")
    )

    private class FakeHolidaysApi : HolidaysApi {
        var result: List<PublicHolidayDto> = emptyList()
        var exception: Throwable? = null

        override suspend fun getPublicHolidays(
            year: Int,
            countryCode: String
        ): List<PublicHolidayDto> {
            exception?.let { throw it }
            return result
        }
    }
}