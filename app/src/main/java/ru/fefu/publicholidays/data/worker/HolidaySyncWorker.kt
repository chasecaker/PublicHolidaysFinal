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
import ru.fefu.publicholidays.data.local.UserSettingsManager
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
    private val cachedHolidayDao: CachedHolidayDao,
    private val userSettingsManager: UserSettingsManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            deleteExpiredCache()
            syncCurrentUserCountry()
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

    private suspend fun syncCurrentUserCountry() {
        val currentUserId = userSettingsManager.currentUserId.first()
        val countryCode = if (currentUserId != null) {
            userDao.getUserById(currentUserId)?.defaultCountryCode ?: DEFAULT_COUNTRY
        } else {
            DEFAULT_COUNTRY
        }

        val currentYear = Year.now().value

        syncYear(
            countryCode = countryCode,
            year = currentYear
        )

        syncYear(
            countryCode = countryCode,
            year = currentYear + 1
        )
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
            cachedAt = cachedAt
        )
    }

    private companion object {
        private const val TAG = "HolidaySyncWorker"
        private const val DEFAULT_COUNTRY = "RU"
        private val CACHE_MAX_AGE_MILLIS = TimeUnit.DAYS.toMillis(30)
    }
}