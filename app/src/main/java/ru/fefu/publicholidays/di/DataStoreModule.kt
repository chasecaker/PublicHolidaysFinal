package ru.fefu.publicholidays.di

import android.content.Context
import ru.fefu.publicholidays.data.local.UserSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideUserSettingsManager(
        @ApplicationContext context: Context
    ): UserSettingsManager {
        return UserSettingsManager(context)
    }
}