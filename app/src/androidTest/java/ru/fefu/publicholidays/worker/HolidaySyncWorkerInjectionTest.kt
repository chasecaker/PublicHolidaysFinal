package ru.fefu.publicholidays.worker

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.fefu.publicholidays.data.worker.HolidaySyncWorker
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HolidaySyncWorkerInjectionTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun holidayWorker_createdByHiltWorkerFactory() {
        val worker = TestListenableWorkerBuilder<HolidaySyncWorker>(context)
            .setWorkerFactory(workerFactory)
            .build()

        assertNotNull(worker)
    }
}