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
import com.example.comw.ui.viewmodel.PeopleListViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.PersonItemCard

private const val VARIANT_CODE = "TVM-PEOPLE-MOD_E25_FAV_SORT"

@Composable
fun PeopleListScreen(
    onOpenDetail: (Long) -> Unit,
    viewModel: PeopleListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = "TVmaze People",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "VariantCode: $VARIANT_CODE",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (favorites.isNotEmpty()) {
            Text(
                text = "Favorites (sorted by adding time)",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 220.dp)
            ) {
                items(favorites, key = { it.person.id }) { entry ->
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
                    CircularProgressIndicator()
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
                            isFavorite = viewModel.isFavorite(item.id),
                            onFavoriteClick = {
                                if (viewModel.isFavorite(item.id))
                                    viewModel.removeFavorite(item.id)
                                else
                                    viewModel.addFavorite(item)
                            },
                            onClick = { onOpenDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}
