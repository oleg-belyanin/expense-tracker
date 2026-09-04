package com.olegbelyanin.expensetracker.data.filters

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.olegbelyanin.expensetracker.domain.expense.ExpenseListFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.expenseListFilterDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "expense_list_filter",
)

interface ExpenseListFilterStore {
    suspend fun load(): ExpenseListFilter

    suspend fun save(filter: ExpenseListFilter)
}

class ExpenseListFilterRepository(context: Context) : ExpenseListFilterStore {
    private val dataStore = context.applicationContext.expenseListFilterDataStore

    override suspend fun load(): ExpenseListFilter = dataStore.data.map { preferences ->
        ExpenseListFilterStorage.decode(
            ExpenseListFilterRecord(
                preset = preferences[PRESET_KEY],
                customStart = preferences[CUSTOM_START_KEY],
                customEnd = preferences[CUSTOM_END_KEY],
                categoryIds = preferences[CATEGORY_IDS_KEY],
                locationId = preferences[LOCATION_ID_KEY],
            ),
        )
    }.first()

    override suspend fun save(filter: ExpenseListFilter) {
        val record = ExpenseListFilterStorage.encode(filter)
        dataStore.edit { preferences ->
            preferences[PRESET_KEY] = record.preset.orEmpty()
            if (record.customStart == null) {
                preferences.remove(CUSTOM_START_KEY)
            } else {
                preferences[CUSTOM_START_KEY] = record.customStart
            }
            if (record.customEnd == null) {
                preferences.remove(CUSTOM_END_KEY)
            } else {
                preferences[CUSTOM_END_KEY] = record.customEnd
            }
            preferences[CATEGORY_IDS_KEY] = record.categoryIds.orEmpty()
            val locationId = record.locationId
            if (locationId == null) {
                preferences.remove(LOCATION_ID_KEY)
            } else {
                preferences[LOCATION_ID_KEY] = locationId
            }
        }
    }

    private companion object {
        val PRESET_KEY = stringPreferencesKey("filter_preset")
        val CUSTOM_START_KEY = stringPreferencesKey("filter_custom_start")
        val CUSTOM_END_KEY = stringPreferencesKey("filter_custom_end")
        val CATEGORY_IDS_KEY = stringPreferencesKey("filter_category_ids")
        val LOCATION_ID_KEY = longPreferencesKey("filter_location_id")
    }
}
