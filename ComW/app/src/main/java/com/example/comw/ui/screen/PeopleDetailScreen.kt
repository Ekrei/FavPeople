package com.example.comw.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.comw.ui.viewmodel.PeopleDetailViewModel
import com.example.comw.ui.viewmodel.UiState

@Composable
fun PeopleDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: PeopleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(itemId) {
        viewModel.load(itemId)
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
    ) {
        Text(
            text = "Detail (id=$itemId)",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (val s = state) {
            UiState.Loading -> {
                Text("Loading...")
            }
            is UiState.Error -> {
                Text(s.message)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.load(itemId) }) {
                    Text("Retry")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
            is UiState.Content -> {
                val d = s.data
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        d.imageUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = d.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Country: ${d.country ?: "—"}")
                        Text("Birthday: ${d.birthday ?: "—"}")
                        d.deathday?.let { Text("Death: $it") }
                        Text("Gender: ${d.gender ?: "—"}")
                        Text("URL: ${d.url ?: "—"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        val isFav = viewModel.isFavorite(d.id)
                        Button(
                            onClick = {
                                if (isFav) viewModel.removeFromFavorites(d.id)
                                else viewModel.addToFavorites(d)
                            }
                        ) {
                            Text(if (isFav) "Unfavorites" else "Favorite")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}
