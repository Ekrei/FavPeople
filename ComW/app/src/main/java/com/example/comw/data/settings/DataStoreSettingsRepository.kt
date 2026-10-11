package com.example.comw.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.AppSettings
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Настройки в DataStore Preferences: файл "ключ -> значение" с асинхронным API на Flow.
 * Здесь только ПРЕДПОЧТЕНИЯ, пользовательские данные (заметки, подборки, история) — в Room.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val TTL = intPreferencesKey("cache_ttl_minutes")
        val BACKGROUND_SYNC = booleanPreferencesKey("background_sync")
        val WIFI_ONLY = booleanPreferencesKey("sync_wifi_only")
        val SHOW_HIDDEN = booleanPreferencesKey("show_hidden")
        val SORT = stringPreferencesKey("sort_order")
        val THEME = stringPreferencesKey("theme_mode")
        val LAST_SYNC = longPreferencesKey("last_sync_at")
    }

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                cacheTtlMinutes = prefs[Keys.TTL] ?: AppSettings.DEFAULT_TTL_MINUTES,
                backgroundSyncEnabled = prefs[Keys.BACKGROUND_SYNC] ?: true,
                syncOnlyOnWifi = prefs[Keys.WIFI_ONLY] ?: false,
                showHidden = prefs[Keys.SHOW_HIDDEN] ?: false,
                sortOrder = SortOrder.entries.firstOrNull { it.name == prefs[Keys.SORT] } ?: SortOrder.API,
                themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[Keys.THEME] } ?: ThemeMode.SYSTEM,
                lastSyncAt = prefs[Keys.LAST_SYNC]
            )
        }
        .distinctUntilChanged()

    override suspend fun setCacheTtlMinutes(minutes: Int) {
        dataStore.edit { it[Keys.TTL] = minutes }
    }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BACKGROUND_SYNC] = enabled }
    }

    override suspend fun setSyncOnlyOnWifi(enabled: Boolean) {
        dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    override suspend fun setShowHidden(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_HIDDEN] = show }
    }

    override suspend fun setSortOrder(order: SortOrder) {
        dataStore.edit { it[Keys.SORT] = order.name }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    override suspend fun setLastSyncAt(time: Long) {
        dataStore.edit { it[Keys.LAST_SYNC] = time }
    }
}
