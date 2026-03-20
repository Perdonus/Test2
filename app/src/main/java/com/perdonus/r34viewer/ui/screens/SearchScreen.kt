package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadState.NotLoading
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.data.settings.PreferenceCatalogItem
import com.perdonus.r34viewer.ui.components.PostCard
import com.perdonus.r34viewer.ui.components.ScreenHeader
import okhttp3.OkHttpClient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    hasSubmittedSearch: Boolean,
    pagingItems: LazyPagingItems<Rule34Post>,
    gridState: LazyGridState,
    favoriteIds: Set<String>,
    settings: AppSettings,
    searchSuggestions: List<PreferenceCatalogItem>,
    feedbackMessage: String?,
    isResolvingQuery: Boolean,
    okHttpClient: OkHttpClient,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onAiSearch: () -> Unit,
    onSaveSearch: () -> Unit,
    onToggleAiFilter: (Boolean) -> Unit,
    onSelectService: (BooruService) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPost: (Rule34Post) -> Unit,
    onToggleFavorite: (Rule34Post) -> Unit,
    onUseSuggestion: (String) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showAiDialog by rememberSaveable { mutableStateOf(false) }
    var isSearchFieldFocused by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "rule34",
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Настройки")
                }
            },
        )

        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            state = gridState,
            columns = GridCells.Adaptive(170.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSearchFieldFocused = it.isFocused },
                        value = query,
                        onValueChange = onQueryChanged,
                        singleLine = true,
                        label = { Text("Поиск по имени или тегам") },
                        trailingIcon = {
                            IconButton(onClick = onSearch, enabled = !isResolvingQuery) {
                                Icon(Icons.Outlined.Search, contentDescription = "Запустить поиск")
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    )

                    Text(
                        text = "Оставьте поле пустым, чтобы открыть свежие посты выбранного сервиса.",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    if (isSearchFieldFocused && query.isNotBlank() && searchSuggestions.isNotEmpty()) {
                        SearchSuggestionSheet(
                            suggestions = searchSuggestions,
                            onUseSuggestion = { suggestion ->
                                isSearchFieldFocused = false
                                onUseSuggestion(suggestion)
                            },
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SearchActionButton(
                            enabled = !isResolvingQuery,
                            onClick = onSearch,
                            onLongClick = {
                                if (query.isNotBlank()) {
                                    showAiDialog = true
                                }
                            },
                            text = if (isResolvingQuery) "Ищу..." else "Поиск",
                        )

                        Button(onClick = onSaveSearch, enabled = query.isNotBlank() && !isResolvingQuery) {
                            Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                            Text(" Сохранить запрос")
                        }

                        FilterChip(
                            selected = settings.hideAiContent,
                            onClick = { onToggleAiFilter(!settings.hideAiContent) },
                            label = {
                                Text("Скрывать ИИ-посты")
                            },
                        )
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        BooruService.entries.forEach { service ->
                            FilterChip(
                                selected = settings.selectedService == service,
                                onClick = { onSelectService(service) },
                                label = { Text(service.displayName) },
                            )
                        }
                    }

                    feedbackMessage?.let { message ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(message, style = MaterialTheme.typography.bodyMedium)
                                ElevatedAssistChip(
                                    onClick = onDismissMessage,
                                    label = { Text("Скрыть") },
                                )
                            }
                        }
                    }
                }
            }

            when {
                !hasSubmittedSearch -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "Поиск и лента",
                            subtitle = if (query.isBlank()) {
                                "Введите имя, теги или оставьте поле пустым. Поиск стартует только по кнопке «Поиск», а пустой запрос открывает свежие посты текущего сервиса."
                            } else {
                                "Запрос введён, но поиск ещё не запускался. Нажмите «Поиск» или удерживайте кнопку для ИИ-поиска."
                            },
                        )
                    }
                }

                pagingItems.loadState.refresh is LoadState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                pagingItems.loadState.refresh is LoadState.Error -> {
                    val message = (pagingItems.loadState.refresh as LoadState.Error).error.message.orEmpty()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "Не удалось загрузить посты",
                            subtitle = message.ifBlank { "Проверьте прокси или переключите сервис." },
                        )
                    }
                }

                pagingItems.itemCount == 0 &&
                    (pagingItems.loadState.append as? NotLoading)?.endOfPaginationReached == true -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "Ничего не найдено",
                            subtitle = if (query.isBlank()) {
                                "У выбранного сервиса сейчас нет доступных свежих постов."
                            } else {
                                "Попробуйте другие теги, ИИ-поиск или другой сервис."
                            },
                        )
                    }
                }

                else -> {
                    items(
                        count = pagingItems.itemCount,
                        key = pagingItems.itemKey { it.serviceScopedId },
                        contentType = pagingItems.itemContentType { it.mediaType.name },
                    ) { index ->
                        val post = pagingItems[index] ?: return@items
                        PostCard(
                            post = post,
                            isFavorite = post.serviceScopedId in favoriteIds,
                            okHttpClient = okHttpClient,
                            onOpenPost = onOpenPost,
                            onToggleFavorite = onToggleFavorite,
                        )
                    }

                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("ИИ-поиск") },
            text = {
                Text(
                    "Попросить сервер найти персонажа, сверить теги на booru и подобрать запрос для ${settings.selectedService.displayName}?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiDialog = false
                        onAiSearch()
                    },
                ) {
                    Text("Запустить ИИ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@Composable
private fun SearchSuggestionSheet(
    suggestions: List<PreferenceCatalogItem>,
    onUseSuggestion: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(suggestions, key = { it.tag }) { suggestion ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUseSuggestion(suggestion.tag) },
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = suggestion.titleRu,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = suggestion.tag,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Постов: ${suggestion.postCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.combinedClickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(text)
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
            )
        }
    }
}
