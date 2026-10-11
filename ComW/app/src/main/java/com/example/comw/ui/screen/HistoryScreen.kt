package com.example.comw.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.ui.util.formatDateTime
import com.example.comw.ui.viewmodel.HistoryViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.PersonItemCard

/** Экран истории просмотров (данные из Room, работает и без интернета). */
@Composable
fun HistoryScreen(
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Text(
                text = "История просмотров",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (state is UiState.Content) {
                TextButton(onClick = { viewModel.clear() }) { Text("Очистить") }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (val s = state) {
            UiState.Loading -> CircularProgressIndicator()
            UiState.Empty -> Text("Вы ещё никого не открывали")
            is UiState.Error -> Text(s.message)
            is UiState.Content -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.data, key = { it.person.id }) { entry ->
                        Column {
                            Text(
                                text = "Просмотрено: ${entry.viewedAtMillis.formatDateTime()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            PersonItemCard(
                                item = entry.person,
                                isFavorite = false,
                                showFavoriteButton = false,
                                onFavoriteClick = {},
                                onClick = { onOpenDetail(entry.person.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
