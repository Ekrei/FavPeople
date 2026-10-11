package com.example.comw.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.comw.domain.logic.CollectionNameValidator
import com.example.comw.model.CollectionDetails
import com.example.comw.ui.viewmodel.CollectionViewModel
import com.example.comw.ui.viewmodel.UiState
import com.example.comw.ui.widget.PersonItemCard
import com.example.comw.ui.widget.ScreenHeader
import com.example.comw.ui.widget.TextInputDialog

@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            when (val s = state) {
                UiState.Loading -> {
                    ScreenHeader(title = "Подборка", onBack = onBack)
                    CircularProgressIndicator()
                }
                UiState.Empty, is UiState.Error -> {
                    ScreenHeader(title = "Подборка", onBack = onBack)
                    Text("Подборка не найдена (возможно, удалена)")
                }
                is UiState.Content -> {
                    val details = s.data
                    ScreenHeader(title = "📁 ${details.name}", onBack = onBack) {
                        TextButton(onClick = { showEdit = true }) { Text("Изменить") }
                    }
                    if (details.description.isNotBlank()) {
                        Text(details.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Людей: ${details.members.size}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { confirmDelete = true }) { Text("Удалить подборку") }
                    }
                    MembersList(details, onOpenDetail, onRemove = viewModel::removePerson)
                    if (showEdit) {
                        TextInputDialog(
                            title = "Изменить подборку",
                            initialName = details.name,
                            initialDescription = details.description,
                            // своё же имя не считается "занятым" — проверку повторов делает репозиторий
                            validate = { CollectionNameValidator.validate(it, emptyList()) },
                            onConfirm = { name, description ->
                                viewModel.update(name, description)
                                showEdit = false
                            },
                            onDismiss = { showEdit = false }
                        )
                    }
                }
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить подборку?") },
            text = { Text("Люди останутся в приложении, удалится только подборка.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun MembersList(
    details: CollectionDetails,
    onOpenDetail: (Long) -> Unit,
    onRemove: (Long) -> Unit
) {
    if (details.members.isEmpty()) {
        Text(
            "Пока пусто — добавляйте людей с экрана деталей (раздел «Мои подборки»)",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(details.members, key = { it.id }) { person ->
            Column {
                PersonItemCard(
                    item = person,
                    isFavorite = false,
                    showFavoriteButton = false,
                    onFavoriteClick = {},
                    onClick = { onOpenDetail(person.id) }
                )
                TextButton(onClick = { onRemove(person.id) }) { Text("Убрать из подборки") }
            }
        }
    }
}
