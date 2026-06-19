package ru.fefu.publicholidays.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import ru.fefu.publicholidays.data.local.CachedHolidayDao
import ru.fefu.publicholidays.data.local.CachedHolidayEntity
import ru.fefu.publicholidays.data.local.UserDao
import ru.fefu.publicholidays.data.model.PublicHolidayDto
import ru.fefu.publicholidays.data.remote.HolidaysApi
import java.time.Year
import java.util.concurrent.TimeUnit

@HiltWorker
class HolidaySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: HolidaysApi,
    private val userDao: UserDao,
    private val cachedHolidayDao: CachedHolidayDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            deleteExpiredCache()
            syncAllUserCountries()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Holiday sync failed", e)
            Result.retry()
        }
    }

    private suspend fun deleteExpiredCache() {
        val expirationTime = System.currentTimeMillis() - CACHE_MAX_AGE_MILLIS
        cachedHolidayDao.deleteExpiredCache(expirationTime)
    }

    private suspend fun syncAllUserCountries() {
        val allUsers = userDao.getAllUsers().first()

        val countryCodes = allUsers.map { it.defaultCountryCode }.distinct()

        val targetCountries = if (countryCodes.isEmpty()) {
            listOf(DEFAULT_COUNTRY)
        } else {
            countryCodes
        }

        val currentYear = Year.now().value

        targetCountries.forEach { countryCode ->
            syncYear(
                countryCode = countryCode,
                year = currentYear
            )
            syncYear(
                countryCode = countryCode,
                year = currentYear + 1
            )
        }
    }

    private suspend fun syncYear(
        countryCode: String,
        year: Int
    ) {
        val remoteHolidays = api.getPublicHolidays(
            year = year,
            countryCode = countryCode
        )

        if (remoteHolidays.isEmpty()) return

        val now = System.currentTimeMillis()
        val cachedEntities = remoteHolidays.map { dto ->
            dto.toCachedEntity(
                year = year,
                cachedAt = now
            )
        }

        cachedHolidayDao.clearCache(
            countryCode = countryCode,
            year = year
        )
        cachedHolidayDao.insertCachedHolidays(cachedEntities)
    }

    private fun PublicHolidayDto.toCachedEntity(
        year: Int,
        cachedAt: Long
    ): CachedHolidayEntity {
        return CachedHolidayEntity(
            holidayId = "$date|$countryCode|$name",
            date = date,
            localName = localName,
            name = name,
            countryCode = countryCode,
            year = year,
            fixed = fixed,
            global = global,
            counties = counties,
            launchYear = launchYear,
            types = types,
            cachedAt = cachedAt
        )
    }

    private companion object {
        private const val TAG = "HolidaySyncWorker"
        private const val DEFAULT_COUNTRY = "RU"
        private val CACHE_MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(30)
    }
}