package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.data.local.SavedSearchEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedSearchesScreen(
    savedSearches: List<SavedSearchEntity>,
    onRunSearch: (SavedSearchEntity) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var renamingSearch by remember { mutableStateOf<SavedSearchEntity?>(null) }
    var renameValue by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CenterAlignedTopAppBar(title = { Text("Закладки") })
        }

        if (savedSearches.isEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyState(
                        title = "Закладок пока нет",
                        subtitle = "Сохраните любой поисковый запрос из экрана поиска.",
                    )
                }
            }
        } else {
            items(savedSearches, key = { it.id }) { search ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(search.label, style = MaterialTheme.typography.titleMedium)
                            Text(search.service.displayName, style = MaterialTheme.typography.labelMedium)
                            Text(search.query, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row {
                            IconButton(onClick = { onRunSearch(search) }) {
                                Icon(Icons.Outlined.PlayArrow, contentDescription = "Запустить поиск")
                            }
                            IconButton(
                                onClick = {
                                    renamingSearch = search
                                    renameValue = search.label
                                },
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Переименовать")
                            }
                            IconButton(onClick = { onDelete(search.id) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }
    }

    renamingSearch?.let { current ->
        AlertDialog(
            onDismissRequest = { renamingSearch = null },
            title = { Text("Переименовать закладку") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Название") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRename(current.id, renameValue.trim())
                        renamingSearch = null
                    },
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingSearch = null }) {
                    Text("Отмена")
                }
            },
        )
    }
}
