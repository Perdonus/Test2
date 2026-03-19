package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.ui.components.PostCard
import com.perdonus.r34viewer.ui.components.ScreenHeader
import okhttp3.OkHttpClient

@Composable
fun FavoritesScreen(
    favorites: List<Rule34Post>,
    okHttpClient: OkHttpClient,
    onOpenPost: (Rule34Post) -> Unit,
    onToggleFavorite: (Rule34Post) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Избранное")

        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Adaptive(170.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (favorites.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = "Избранное пока пустое",
                        subtitle = "Добавляйте понравившиеся арты и видео из ленты или экрана поста.",
                    )
                }
            } else {
                items(
                    items = favorites,
                    key = { it.serviceScopedId },
                ) { post ->
                    PostCard(
                        post = post,
                        isFavorite = true,
                        okHttpClient = okHttpClient,
                        onOpenPost = onOpenPost,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}
