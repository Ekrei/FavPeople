package com.example.comw.ui.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.comw.model.PersonalStatus
import com.example.comw.model.TvmPerson

@Composable
fun PersonItemCard(
    item: TvmPerson,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    showFavoriteButton: Boolean = true,
    status: PersonalStatus = PersonalStatus.NONE,
    hasNote: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item.imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                // личные отметки пользователя
                val marks = listOfNotNull(
                    status.badge.takeIf { it.isNotEmpty() }?.let { "$it ${status.label}" },
                    if (hasNote) "📝 есть заметка" else null
                )
                if (marks.isNotEmpty()) {
                    Text(text = marks.joinToString("  ·  "), style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(4.dp))
                item.country?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                item.birthday?.let { Text(text = "Born: $it", style = MaterialTheme.typography.bodySmall) }
                Text(text = "ID: ${item.id}", style = MaterialTheme.typography.bodySmall)
            }
            if (showFavoriteButton) {
                IconButton(onClick = onFavoriteClick) {
                    Text(if (isFavorite) "★" else "☆", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
