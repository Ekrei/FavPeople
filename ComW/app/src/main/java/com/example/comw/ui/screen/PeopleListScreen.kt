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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.ui.viewmodel.GenderFilter
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.PersonItemCard

private const val VARIANT_CODE = "TVM-PEOPLE-MOD_E25_FAV_SORT"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleListScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: PeopleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TVmaze People",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenHistory) { Text("История") }
        }
        Text(
            text = "VariantCode: $VARIANT_CODE",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Источник 1: строка поиска
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

        // Источник 2: фильтр
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenderFilter.entries.forEach { option ->
                FilterChip(
                    selected = state.filter == option,
                    onClick = { viewModel.onFilterChange(option) },
                    label = { Text(option.label) }
                )
            }
        }

        // Источник 3: избранное из Room (обновляется само)
        if (state.favorites.isNotEmpty()) {
            Text(
                text = "Избранное (по времени добавления)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 180.dp)
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
                        text = if (state.query.isBlank()) "Список пуст"
                        else "По запросу «${state.query.trim()}» никого не найдено"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Источник 4: поток действий пользователя
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
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
                            onFavoriteClick = { viewModel.toggleFavorite(row.person) },
                            onClick = { onOpenDetail(row.person.id) }
                        )
                    }
                }
            }
        }
    }
}
