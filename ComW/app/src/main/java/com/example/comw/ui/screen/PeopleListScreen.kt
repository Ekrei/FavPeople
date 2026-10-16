package com.example.comw.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.domain.logic.ListFilter
import com.example.comw.ui.util.formatDateTime
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.OfflineBanner
import com.example.comw.ui.widget.PersonItemCard

private const val VARIANT_CODE = "TVM-PEOPLE-MOD_E25_FAV_SORT"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleListScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMy: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    pendingQuery: String? = null,
    onPendingQueryConsumed: () -> Unit = {},
    viewModel: PeopleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

    // Запрос, выбранный на экране истории
    LaunchedEffect(pendingQuery) {
        if (pendingQuery != null) {
            viewModel.onQueryChange(pendingQuery)
            onPendingQueryConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(text = "TVmaze People", style = MaterialTheme.typography.headlineSmall)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(onClick = onOpenMy) { Text("Моё") }
            TextButton(onClick = onOpenHistory) { Text("История") }
            TextButton(onClick = onOpenSettings) { Text("Настройки") }
        }
        Text(
            text = "VariantCode: $VARIANT_CODE",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text("Поиск по имени") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { viewModel.onQueryChange("") }) { Text("✕") }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search")
        )

        // недавние запросы (Room)
        if (query.isEmpty() && state.recentSearches.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.recentSearches.forEach { recent ->
                    AssistChip(
                        onClick = { viewModel.onQueryChange(recent) },
                        label = { Text("🕘 $recent") }
                    )
                }
            }
        }

        // фильтры (часть зависит от личных данных)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            ListFilter.entries.forEach { option ->
                FilterChip(
                    selected = state.filter == option,
                    onClick = { viewModel.onFilterChange(option) },
                    label = { Text(option.label) }
                )
            }
        }

        state.notice?.let { OfflineBanner(text = it, onRetry = { viewModel.refresh() }) }

        if (state.query.isBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.lastUpdatedAt?.let { "Обновлено: ${it.formatDateTime()}" }
                        ?: "Ещё не обновлялось",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = { viewModel.refresh() }) { Text("Обновить") }
                }
            }
        }

        if (state.favorites.isNotEmpty() && state.filter != ListFilter.FAVORITES && state.query.isBlank()) {
            Text(
                text = "Избранное (по времени добавления)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 160.dp)
            ) {
                items(state.favorites, key = { "fav_${it.person.id}" }) { entry ->
                    PersonItemCard(
                        item = entry.person,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.removeFavorite(entry.person.id) },
                        onClick = { onOpenDetail(entry.person.id) }
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        when (val s = state.content) {
            UiState.Loading -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading"))
                }
            }
            UiState.Empty -> {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when {
                            state.query.isNotBlank() -> "По запросу «${state.query.trim()}» никого не найдено"
                            state.filter != ListFilter.ALL -> "Под фильтр «${state.filter.label}» никто не подходит"
                            else -> "Список пуст"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) { Text("Обновить") }
                }
            }
            is UiState.Error -> {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = s.message)
                    Text(
                        text = "Сохранённых данных пока нет",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                }
            }
            is UiState.Content -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.data, key = { it.person.id }) { row ->
                        PersonItemCard(
                            item = row.person,
                            isFavorite = row.isFavorite,
                            status = row.status,
                            hasNote = row.hasNote,
                            onFavoriteClick = { viewModel.toggleFavorite(row.person) },
                            onClick = { onOpenDetail(row.person.id) }
                        )
                    }
                }
            }
        }
    }
}
