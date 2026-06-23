package ru.fefu.publicholidays.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

@Singleton
class UserSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val currentUserKey = stringPreferencesKey("current_user_id")
    private val darkThemeKey = booleanPreferencesKey("dark_theme_enabled")

    val currentUserId: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[currentUserKey] }

    val isDarkThemeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[darkThemeKey] ?: false }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { preferences ->
            if (userId != null) {
                preferences[currentUserKey] = userId
            } else {
                preferences.remove(currentUserKey)
            }
        }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[darkThemeKey] = enabled
        }
    }
}