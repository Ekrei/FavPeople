package com.example.comw.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.comw.data.sync.SyncScheduler
import com.example.comw.domain.repository.PeopleRepository
import com.example.comw.domain.repository.SettingsRepository
import com.example.comw.model.AppSettings
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val peopleRepository: PeopleRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val cachedPeopleCount: StateFlow<Int> = peopleRepository.observeCachedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun setTtl(minutes: Int) = runInScope { settingsRepository.setCacheTtlMinutes(minutes) }
    fun setBackgroundSync(enabled: Boolean) = runInScope { settingsRepository.setBackgroundSyncEnabled(enabled) }
    fun setWifiOnly(enabled: Boolean) = runInScope { settingsRepository.setSyncOnlyOnWifi(enabled) }
    fun setShowHidden(show: Boolean) = runInScope { settingsRepository.setShowHidden(show) }
    fun setSortOrder(order: SortOrder) = runInScope { settingsRepository.setSortOrder(order) }
    fun setThemeMode(mode: ThemeMode) = runInScope { settingsRepository.setThemeMode(mode) }

    fun syncNow() {
        syncScheduler.syncNow()
        _messages.tryEmit("Синхронизация запущена — выполнится, когда будет сеть")
    }

    fun clearCache() = runInScope {
        peopleRepository.clearCache()
        _messages.tryEmit("Кэш очищен (личные данные сохранены)")
    }

    private fun runInScope(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
