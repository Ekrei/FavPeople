package com.example.comw.ui.screen

import androidx.compose.foundation.clickable
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
import com.example.comw.model.CollectionSummary
import com.example.comw.model.NoteWithPerson
import com.example.comw.ui.util.formatDateTime
import com.example.comw.ui.viewmodel.MyViewModel
import com.example.comw.ui.widget.ScreenHeader
import com.example.comw.ui.widget.SectionTitle
import com.example.comw.ui.widget.TextInputDialog

/** Личный раздел: подборки и заметки пользователя. Всё хранится в Room и доступно офлайн. */
@Composable
fun MyScreen(
    onBack: () -> Unit,
    onOpenCollection: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: MyViewModel = hiltViewModel()
) {
    val collections by viewModel.collections.collectAsState()
    val notes by viewModel.notes.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CollectionSummary?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { ScreenHeader(title = "Моё", onBack = onBack) }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Мои подборки (${collections.size})", modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCreate = true }) { Text("+ Создать") }
                }
            }
            if (collections.isEmpty()) {
                item {
                    Text(
                        "Создайте подборку, например «Любимые актёры», и добавляйте людей с экрана деталей",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(collections, key = { "c_${it.id}" }) { collection ->
                CollectionCard(
                    collection = collection,
                    onClick = { onOpenCollection(collection.id) },
                    onDelete = { toDelete = collection }
                )
            }

            item { SectionTitle("Мои заметки и статусы (${notes.size})") }
            if (notes.isEmpty()) {
                item {
                    Text(
                        "Откройте человека и поставьте статус или напишите заметку",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            items(notes, key = { "n_${it.note.personId}" }) { note ->
                NoteCard(note = note, onClick = { onOpenDetail(note.note.personId) })
            }
        }
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showCreate) {
        val existing = collections.map { it.name }
        TextInputDialog(
            title = "Новая подборка",
            initialDescription = "",
            confirmText = "Создать",
            validate = { CollectionNameValidator.validate(it, existing) },
            onConfirm = { name, description ->
                viewModel.createCollection(name, description)
                showCreate = false
            },
            onDismiss = { showCreate = false }
        )
    }

    toDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Удалить подборку «${collection.name}»?") },
            text = { Text("Люди останутся в приложении, удалится только подборка.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCollection(collection.id)
                    toDelete = null
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun CollectionCard(collection: CollectionSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("📁 ${collection.name}", style = MaterialTheme.typography.titleMedium)
                if (collection.description.isNotBlank()) {
                    Text(collection.description, style = MaterialTheme.typography.bodySmall)
                }
                Text("Людей: ${collection.itemCount}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onDelete) { Text("Удалить") }
        }
    }
}

@Composable
private fun NoteCard(note: NoteWithPerson, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = note.personName ?: "Человек #${note.note.personId}",
                style = MaterialTheme.typography.titleMedium
            )
            if (note.note.status.badge.isNotEmpty()) {
                Text("${note.note.status.badge} ${note.note.status.label}", style = MaterialTheme.typography.labelMedium)
            }
            if (note.note.text.isNotBlank()) {
                Text("📝 ${note.note.text}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "изменено ${note.note.updatedAt.formatDateTime()}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
