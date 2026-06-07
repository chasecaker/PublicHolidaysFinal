package ru.fefu.publicholidays.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import ru.fefu.publicholidays.data.local.CachedHolidayDao
import ru.fefu.publicholidays.data.local.UserDao
import ru.fefu.publicholidays.data.local.UserSettingsManager
import ru.fefu.publicholidays.data.remote.HolidaysApi
import ru.fefu.publicholidays.data.worker.HolidaySyncWorker

class TestHolidaySyncWorkerFactory(
    private val api: HolidaysApi,
    private val userDao: UserDao,
    private val cachedHolidayDao: CachedHolidayDao,
    private val userSettingsManager: UserSettingsManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return if (workerClassName == HolidaySyncWorker::class.java.name) {
            HolidaySyncWorker(
                context = appContext,
                workerParams = workerParameters,
                api = api,
                userDao = userDao,
                cachedHolidayDao = cachedHolidayDao,
                userSettingsManager = userSettingsManager
            )
        } else {
            null
        }
    }
}