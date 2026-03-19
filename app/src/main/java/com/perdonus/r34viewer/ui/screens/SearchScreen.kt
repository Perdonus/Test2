package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadState.NotLoading
import androidx.paging.compose.LazyPagingItems
import coil.ImageLoader
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.ui.components.PostCard

@Composable
fun SearchScreen(
    query: String,
    pagingItems: LazyPagingItems<Rule34Post>,
    favoriteIds: Set<Int>,
    settings: AppSettings,
    feedbackMessage: String?,
    imageLoader: ImageLoader,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSaveSearch: () -> Unit,
    onToggleAiFilter: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPost: (Rule34Post) -> Unit,
    onToggleFavorite: (Rule34Post) -> Unit,
    onDismissMessage: () -> Unit,
) {
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
                    Text("R34 Native")
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
                    label = { Text("Search tags") },
                    leadingIcon = {
                        Icon(Icons.Outlined.ManageSearch, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Outlined.ManageSearch, contentDescription = "Run search")
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onSearch) {
                        Text("Search")
                    }
                    Button(onClick = onSaveSearch, enabled = query.isNotBlank()) {
                        Icon(Icons.Outlined.Bookmarks, contentDescription = null)
                        Text(" Save query")
                    }
                    FilterChip(
                        selected = settings.hideAiContent,
                        onClick = { onToggleAiFilter(!settings.hideAiContent) },
                        label = {
                            Text("Hide AI")
                        },
                    )
                }

                if (!settings.hasApiCredentials) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Нужно заполнить user_id и api_key в настройках, иначе API rule34.xxx не отдаст посты.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            ElevatedAssistChip(
                                onClick = onOpenSettings,
                                label = { Text("Open settings") },
                            )
                        }
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
            query.isBlank() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Поиск по тегам",
                        subtitle = "Введите теги и запускайте поиск. Примеры: `2girls`, `video`, `cat_ears`.",
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
                        subtitle = message.ifBlank { "Проверьте api_key, user_id и proxy." },
                    )
                }
            }

            pagingItems.itemCount == 0 &&
                (pagingItems.loadState.append as? NotLoading)?.endOfPaginationReached == true -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Ничего не найдено",
                        subtitle = "Попробуйте другие теги или отключите AI-фильтр.",
                    )
                }
            }

            else -> {
                items(
                    count = pagingItems.itemCount,
                    key = { index -> pagingItems[index]?.id ?: index },
                ) { index ->
                    val post = pagingItems[index] ?: return@items
                    PostCard(
                        post = post,
                        isFavorite = post.id in favoriteIds,
                        imageLoader = imageLoader,
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
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
