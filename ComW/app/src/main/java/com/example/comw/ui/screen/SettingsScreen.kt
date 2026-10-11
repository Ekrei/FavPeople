package com.example.comw.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.model.AppSettings
import com.example.comw.model.SortOrder
import com.example.comw.model.ThemeMode
import com.example.comw.ui.util.formatDateTime
import com.example.comw.ui.viewmodel.SettingsViewModel
import com.example.comw.ui.widget.ScreenHeader
import com.example.comw.ui.widget.SectionTitle

/** Экран настроек. Все значения хранятся в DataStore и сразу влияют на приложение. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val cachedCount by viewModel.cachedPeopleCount.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            ScreenHeader(title = "Настройки", onBack = onBack)

            SectionTitle("Данные и офлайн")
            Text("Сколько считать данные свежими (TTL кэша):", style = MaterialTheme.typography.bodyMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                AppSettings.TTL_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = settings.cacheTtlMinutes == minutes,
                        onClick = { viewModel.setTtl(minutes) },
                        label = { Text(ttlLabel(minutes)) }
                    )
                }
            }
            Text(
                "Пока данные свежие, приложение не ходит в сеть и сразу показывает кэш.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("В кэше: $cachedCount чел.", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = { viewModel.clearCache() }) { Text("Очистить кэш") }

            SectionTitle("Фоновая синхронизация")
            SwitchRow(
                title = "Обновлять в фоне (раз в 6 часов)",
                subtitle = "Обновляет список и заранее скачивает людей из избранного, подборок и заметок",
                checked = settings.backgroundSyncEnabled,
                onChange = viewModel::setBackgroundSync
            )
            SwitchRow(
                title = "Только по Wi-Fi",
                checked = settings.syncOnlyOnWifi,
                enabled = settings.backgroundSyncEnabled,
                onChange = viewModel::setWifiOnly
            )
            Text(
                text = settings.lastSyncAt?.let { "Последняя синхронизация: ${it.formatDateTime()}" }
                    ?: "Синхронизации ещё не было",
                style = MaterialTheme.typography.bodySmall
            )
            Button(onClick = { viewModel.syncNow() }) { Text("Синхронизировать сейчас") }

            SectionTitle("Список")
            SwitchRow(
                title = "Показывать скрытых людей",
                subtitle = "Люди со статусом «Скрыть из ленты»",
                checked = settings.showHidden,
                onChange = viewModel::setShowHidden
            )
            Text("Сортировка:", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = settings.sortOrder == order,
                        onClick = { viewModel.setSortOrder(order) },
                        label = { Text(order.label) }
                    )
                }
            }

            SectionTitle("Оформление")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

private fun ttlLabel(minutes: Int): String =
    if (minutes < 60) "$minutes мин" else "${minutes / 60} ч"
