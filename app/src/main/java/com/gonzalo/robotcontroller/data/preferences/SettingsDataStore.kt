package com.gonzalo.robotcontroller.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gonzalo.robotcontroller.domain.model.RobotSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "robot_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val RECONNECT_ENABLED = booleanPreferencesKey("reconnect_enabled")
        val MAX_RECONNECT_ATTEMPTS = intPreferencesKey("max_reconnect_attempts")
    }

    val settings: Flow<RobotSettings> = context.dataStore.data.map { preferences ->
        RobotSettings(
            serverUrl = preferences[Keys.SERVER_URL] ?: "ws://10.59.196.87:8765",
            reconnectEnabled = preferences[Keys.RECONNECT_ENABLED] ?: true,
            maxReconnectAttempts = preferences[Keys.MAX_RECONNECT_ATTEMPTS] ?: 5
        )
    }

    suspend fun updateServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SERVER_URL] = url
        }
    }

    suspend fun updateReconnectEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.RECONNECT_ENABLED] = enabled
        }
    }

    suspend fun updateMaxReconnectAttempts(attempts: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.MAX_RECONNECT_ATTEMPTS] = attempts
        }
    }
}
