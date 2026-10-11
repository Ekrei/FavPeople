package com.example.comw.domain.repository

import com.example.comw.model.AppSettings
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Настройки приложения (реализация — DataStore Preferences). */
interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setCacheTtlMinutes(minutes: Int)
    suspend fun setBackgroundSyncEnabled(enabled: Boolean)
    suspend fun setSyncOnlyOnWifi(enabled: Boolean)
    suspend fun setShowHidden(show: Boolean)
    suspend fun setSortOrder(order: SortOrder)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLastSyncAt(time: Long)
}
