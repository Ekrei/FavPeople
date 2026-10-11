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
import com.example.comw.ui.widget.ScreenHeader
import com.example.comw.ui.widget.SectionTitle

/** История (recent): недавние поисковые запросы + просмотренные люди. Всё из Room. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDetail: (Long) -> Unit,
    onSearchClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val searches by viewModel.recentSearches.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { ScreenHeader(title = "История", onBack = onBack) }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Недавние запросы", modifier = Modifier.weight(1f))
                if (searches.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearSearches() }) { Text("Очистить") }
                }
            }
        }
        item {
            if (searches.isEmpty()) {
                Text("Вы ещё ничего не искали", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    searches.forEach { q ->
                        AssistChip(onClick = { onSearchClick(q) }, label = { Text("🔍 $q") })
                    }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Просмотренные", modifier = Modifier.weight(1f))
                if (state is UiState.Content) {
                    TextButton(onClick = { viewModel.clear() }) { Text("Очистить") }
                }
            }
        }
        when (val s = state) {
            UiState.Loading -> item { CircularProgressIndicator() }
            UiState.Empty -> item { Text("Вы ещё никого не открывали") }
            is UiState.Error -> item { Text(s.message) }
            is UiState.Content -> items(s.data, key = { it.person.id }) { entry ->
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
