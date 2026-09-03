package com.olegbelyanin.expensetracker.data.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(context: Context) {
    private val dataStore = context.applicationContext.themeDataStore

    val theme: Flow<ThemePreference> =
        dataStore.data.map { preferences ->
            ThemePreference.fromStorage(preferences[THEME_KEY])
        }

    suspend fun setTheme(preference: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = preference.storageValue
        }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
    }
}
