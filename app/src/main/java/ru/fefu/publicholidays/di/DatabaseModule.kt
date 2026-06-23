package ru.fefu.publicholidays.di

import android.content.Context
import androidx.room.Room
import ru.fefu.publicholidays.data.local.AppDatabase
import ru.fefu.publicholidays.data.local.FavoriteHolidayDao
import ru.fefu.publicholidays.data.local.UserDao
import ru.fefu.publicholidays.data.local.HolidayHistoryDao
import ru.fefu.publicholidays.data.local.HolidayNoteDao
import ru.fefu.publicholidays.data.local.CachedHolidayDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "public_holidays_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFavoriteHolidayDao(database: AppDatabase): FavoriteHolidayDao {
        return database.favoriteHolidayDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideHolidayHistoryDao(database: AppDatabase): HolidayHistoryDao {
        return database.holidayHistoryDao()
    }

    @Provides
    @Singleton
    fun provideHolidayNoteDao(database: AppDatabase): HolidayNoteDao {
        return database.holidayNoteDao()
    }

    @Provides
    @Singleton
    fun provideCachedHolidayDao(database: AppDatabase): CachedHolidayDao {
        return database.cachedHolidayDao()
    }
}