package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadState.NotLoading
import androidx.paging.compose.LazyPagingItems
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.ui.components.PostCard
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    query: String,
    hasSubmittedSearch: Boolean,
    pagingItems: LazyPagingItems<Rule34Post>,
    favoriteIds: Set<String>,
    settings: AppSettings,
    feedbackMessage: String?,
    isAiResolving: Boolean,
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
    onDismissMessage: () -> Unit,
) {
    var showAiDialog by rememberSaveable { mutableStateOf(false) }

    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(170.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CenterAlignedTopAppBar(
                title = {
                    Text("Booru Native")
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = query,
                    onValueChange = onQueryChanged,
                    singleLine = true,
                    label = { Text("Search tags or name") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = onSearch, enabled = !isAiResolving) {
                            Icon(Icons.Outlined.Search, contentDescription = "Run search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SearchActionButton(
                        enabled = !isAiResolving,
                        onClick = onSearch,
                        onLongClick = {
                            if (query.isNotBlank()) {
                                showAiDialog = true
                            }
                        },
                        text = if (isAiResolving) "AI..." else "Search",
                    )

                    Button(onClick = onSaveSearch, enabled = query.isNotBlank() && !isAiResolving) {
                        Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                        Text(" Save query")
                    }

                    FilterChip(
                        selected = settings.hideAiContent,
                        onClick = { onToggleAiFilter(!settings.hideAiContent) },
                        label = {
                            Text("Hide AI posts")
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
                                label = { Text("Hide") },
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
                        title = "Поиск по тегам",
                        subtitle = if (query.isBlank()) {
                            "Введите теги или имя персонажа. Поиск стартует только по кнопке Search. Долгое нажатие на Search запускает AI-подбор тегов."
                        } else {
                            "Запрос введён, но поиск ещё не запускался. Нажмите Search или удерживайте кнопку для AI-поиска."
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
                        subtitle = message.ifBlank { "Проверьте proxy или переключите сервис." },
                    )
                }
            }

            pagingItems.itemCount == 0 &&
                (pagingItems.loadState.append as? NotLoading)?.endOfPaginationReached == true -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Ничего не найдено",
                        subtitle = "Попробуйте другие теги, AI-поиск или другой сервис.",
                    )
                }
            }

            else -> {
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.serviceScopedId ?: index },
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

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            title = { Text("AI search") },
            text = {
                Text(
                    "Преобразовать запрос в booru-теги для ${settings.selectedService.displayName}?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAiDialog = false
                        onAiSearch()
                    },
                ) {
                    Text("Run AI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("Cancel")
                }
            },
        )
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
