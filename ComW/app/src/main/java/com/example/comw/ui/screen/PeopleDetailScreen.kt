package com.example.comw.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.comw.domain.logic.CollectionNameValidator
import com.example.comw.model.PersonalStatus
import com.example.comw.ui.util.formatDateTime
import com.example.comw.ui.viewmodel.DetailUiState
import com.example.comw.ui.viewmodel.PeopleDetailViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.OfflineBanner
import com.example.comw.ui.widget.SectionTitle
import com.example.comw.ui.widget.TextInputDialog

@Composable
fun PeopleDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: PeopleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Назад") }
                Text(
                    text = "Detail (id=$itemId)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }
            state.notice?.let { OfflineBanner(text = it, onRetry = { viewModel.refresh() }) }

            when (val s = state.content) {
                UiState.Loading -> CircularProgressIndicator()
                UiState.Empty -> Text("Нет данных")
                is UiState.Error -> {
                    Text(s.message)
                    Text("Этого человека нет в сохранённых данных", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.refresh() }) { Text("Retry") }
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
                            Text(text = d.name, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Country: ${d.country ?: "—"}")
                            Text("Birthday: ${d.birthday ?: "—"}")
                            d.deathday?.let { Text("Death: $it") }
                            Text("Gender: ${d.gender ?: "—"}")
                            Text("URL: ${d.url ?: "—"}")
                            state.fetchedAt?.let {
                                Text(
                                    text = "Данные от ${it.formatDateTime()}" +
                                        if (state.isRefreshing) " · обновляется…" else "",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.toggleFavorite() }) {
                                    Text(if (state.isFavorite) "★ Убрать из избранного" else "☆ В избранное")
                                }
                                OutlinedButton(onClick = { viewModel.refresh() }) { Text("Обновить") }
                            }
                        }
                    }
                    PersonalSection(state = state, viewModel = viewModel)
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Личная часть: статус, заметка, подборки. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalSection(state: DetailUiState, viewModel: PeopleDetailViewModel) {
    val savedNote = state.note
    // Поля редактирования заполняются сохранённой заметкой и сбрасываются при её изменении.
    var text by rememberSaveable(savedNote?.updatedAt) { mutableStateOf(savedNote?.text ?: "") }
    var status by rememberSaveable(savedNote?.updatedAt) {
        mutableStateOf(savedNote?.status ?: PersonalStatus.NONE)
    }
    var showNewCollection by remember { mutableStateOf(false) }

    SectionTitle("Мой статус")
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        PersonalStatus.entries.forEach { option ->
            FilterChip(
                selected = status == option,
                onClick = { status = option },
                label = { Text("${option.badge} ${option.label}".trim()) }
            )
        }
    }
    if (status == PersonalStatus.HIDDEN) {
        Text(
            "Человек не будет показываться в ленте и поиске (можно включить в настройках)",
            style = MaterialTheme.typography.bodySmall
        )
    }

    SectionTitle("Моя заметка")
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Например: посмотреть фильмографию") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { viewModel.saveNote(text, status) }) { Text("Сохранить") }
        savedNote?.let {
            Text(
                text = "  изменено ${it.updatedAt.formatDateTime()}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }

    SectionTitle("Мои подборки")
    if (state.collections.isEmpty()) {
        Text("Подборок пока нет", style = MaterialTheme.typography.bodySmall)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        state.collections.forEach { choice ->
            FilterChip(
                selected = choice.containsPerson,
                onClick = { viewModel.toggleCollection(choice.collection.id, choice.containsPerson) },
                label = { Text(choice.collection.name) }
            )
        }
        AssistChip(onClick = { showNewCollection = true }, label = { Text("+ Новая") })
    }
    Spacer(modifier = Modifier.height(48.dp))

    if (showNewCollection) {
        val existing = state.collections.map { it.collection.name }
        TextInputDialog(
            title = "Новая подборка",
            confirmText = "Создать и добавить",
            validate = { CollectionNameValidator.validate(it, existing) },
            onConfirm = { name, _ ->
                viewModel.createCollectionAndAdd(name)
                showNewCollection = false
            },
            onDismiss = { showNewCollection = false }
        )
    }
}
