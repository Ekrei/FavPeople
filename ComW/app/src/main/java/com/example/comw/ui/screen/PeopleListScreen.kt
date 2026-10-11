package com.example.comw.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.PersonItemCard

private const val VARIANT_CODE = "TVM-PEOPLE-MOD_E25_FAV_SORT"

@Composable
fun PeopleListScreen(
    onOpenDetail: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: PeopleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.person.id }.toSet() }

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

        if (favorites.isNotEmpty()) {
            Text(
                text = "Избранное (по времени добавления)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 220.dp)
            ) {
                items(favorites, key = { "fav_${it.person.id}" }) { entry ->
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

        when (val s = state) {
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
                    Text(text = "Список пуст")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.load() }) { Text("Обновить") }
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
                    Button(onClick = { viewModel.load() }) {
                        Text("Retry")
                    }
                }
            }
            is UiState.Content -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.data, key = { it.id }) { item ->
                        PersonItemCard(
                            item = item,
                            isFavorite = item.id in favoriteIds,
                            onFavoriteClick = { viewModel.toggleFavorite(item) },
                            onClick = { onOpenDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}
