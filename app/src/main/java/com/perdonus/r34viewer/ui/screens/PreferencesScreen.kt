package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.ui.components.ScreenHeader
import com.perdonus.r34viewer.ui.viewmodel.PreferencesUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onSearch: () -> Unit,
    onAddPreferred: (String) -> Unit,
    onAddBlocked: (String) -> Unit,
    onRemovePreferred: (String) -> Unit,
    onRemoveBlocked: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    LaunchedEffect(state.selectedService) {
        onRefreshCatalog()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Предпочтения",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Что подмешивать в поиск", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Предпочтения применяются ко всем поискам на ${state.selectedService.displayName}. Используются стабильные booru-операторы: обычный тег и исключение через -tag.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                PreferenceSection(
                    title = "Хочу видеть",
                    tags = state.preferredTags,
                    titleByTag = state.titleByTag,
                    emptyText = "Пока ничего не добавлено.",
                    onRemove = onRemovePreferred,
                )
            }

            item {
                PreferenceSection(
                    title = "Не хочу видеть",
                    tags = state.blockedTags,
                    titleByTag = state.titleByTag,
                    emptyText = "Пока ничего не скрывается.",
                    onRemove = onRemoveBlocked,
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Найти тег по API", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = state.catalogQuery,
                            onValueChange = onQueryChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Введите тег или часть тега") },
                            supportingText = { Text("Пустое поле заново загрузит стартовый каталог с сервера.") },
                            trailingIcon = {
                                IconButton(onClick = onSearch, enabled = !state.isSearching) {
                                    Icon(Icons.Outlined.Search, contentDescription = "Искать тег")
                                }
                            },
                        )
                        Button(
                            onClick = onSearch,
                            enabled = !state.isSearching,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.isSearching) "Ищу..." else "Искать теги")
                        }
                    }
                }
            }

            state.message?.let { message ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ElevatedAssistChip(
                                onClick = onDismissMessage,
                                label = { Text("Скрыть") },
                            )
                        }
                    }
                }
            }

            items(state.catalogItems, key = { it.tag }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(item.titleRu, style = MaterialTheme.typography.titleMedium)
                        Text(item.tag, style = MaterialTheme.typography.bodySmall)
                        Text("Постов: ${item.postCount}", style = MaterialTheme.typography.bodySmall)
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = { onAddPreferred(item.tag) },
                                enabled = item.tag !in state.preferredTags,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                Text("Хочу видеть")
                            }
                            Button(
                                onClick = { onAddBlocked(item.tag) },
                                enabled = item.tag !in state.blockedTags,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Block,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                Text("Скрывать")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceSection(
    title: String,
    tags: List<String>,
    titleByTag: Map<String, String>,
    emptyText: String,
    onRemove: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (tags.isEmpty()) {
                Text(emptyText, style = MaterialTheme.typography.bodyMedium)
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        ElevatedAssistChip(
                            onClick = { onRemove(tag) },
                            label = { Text(titleByTag[tag] ?: tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Убрать",
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
