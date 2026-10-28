package com.enonymous1.employeerecords.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.enonymous1.employeerecords.model.DateFormat
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The single settings store, shared by the app and its instrumented tests' factory. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val DATE_FORMAT_KEY = stringPreferencesKey("date_format")

/** The persisted date-format setting. Kept as an interface so tests can fake it. */
interface SettingsRepository {
    val dateFormat: Flow<DateFormat>
    suspend fun setDateFormat(format: DateFormat)
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override val dateFormat: Flow<DateFormat> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> DateFormat.fromLabel(preferences[DATE_FORMAT_KEY]) }

    override suspend fun setDateFormat(format: DateFormat) {
        dataStore.edit { preferences -> preferences[DATE_FORMAT_KEY] = format.label }
    }
}
