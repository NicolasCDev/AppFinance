package com.example.appfinancetest

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "slider_prefs")

class DataStorage(private val context: Context) {
    companion object {
        val START_DATE_KEY = floatPreferencesKey("start_date")
        val END_DATE_KEY = floatPreferencesKey("end_date")
    }

    val startDateFlow: Flow<Float?> = context.dataStore.data.map { it[START_DATE_KEY] }
    val endDateFlow: Flow<Float?> = context.dataStore.data.map { it[END_DATE_KEY] }

    suspend fun saveStartDate(date: Float) {
        context.dataStore.edit { prefs ->
            prefs[START_DATE_KEY] = date
        }
    }

    suspend fun saveEndDate(date: Float) {
        context.dataStore.edit { prefs ->
            prefs[END_DATE_KEY] = date
        }
    }
}