package ru.fefu.publicholidays.worker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.fefu.publicholidays.data.local.AppDatabase
import ru.fefu.publicholidays.data.local.UserEntity
import ru.fefu.publicholidays.data.local.UserSettingsManager
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.remote.HolidaysApi
import ru.fefu.publicholidays.data.worker.HolidaySyncWorker

@RunWith(AndroidJUnit4::class)
class HolidaySyncWorkerTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var api: FakeHolidaysApi
    private lateinit var userSettingsManager: UserSettingsManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        api = FakeHolidaysApi()
        userSettingsManager = UserSettingsManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun holidaySyncWorker_correctlySyncsHolidays() = runBlocking {
        val user = UserEntity(
            userId = "test_user",
            name = "Test User",
            defaultCountryCode = "RU"
        )
        database.userDao().insertUser(user)
        userSettingsManager.setCurrentUserId(user.userId)

        api.result = listOf(
            PublicHolidayDto(
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
        )

        val worker = TestListenableWorkerBuilder<HolidaySyncWorker>(context)
            .setWorkerFactory(
                TestHolidaySyncWorkerFactory(
                    api = api,
                    userDao = database.userDao(),
                    cachedHolidayDao = database.cachedHolidayDao()
                )
            )
            .build()

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)

        val currentYear = java.time.Year.now().value

        val currentYearCache = database.cachedHolidayDao().getCachedHolidays(
            countryCode = "RU",
            year = currentYear
        )

        val nextYearCache = database.cachedHolidayDao().getCachedHolidays(
            countryCode = "RU",
            year = currentYear + 1
        )

        assertEquals(1, currentYearCache.size)
        assertEquals(1, nextYearCache.size)
        assertEquals("New Year's Day", currentYearCache.first().name)
    }

    private class FakeHolidaysApi : HolidaysApi {
        var result: List<PublicHolidayDto> = emptyList()

        override suspend fun getPublicHolidays(
            year: Int,
            countryCode: String
        ): List<PublicHolidayDto> {
            return result.map {
                it.copy(
                    date = "$year-01-01"
                )
            }
        }
    }
}