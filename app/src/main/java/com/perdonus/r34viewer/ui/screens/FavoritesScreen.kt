package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.ui.components.PostCard

@Composable
fun FavoritesScreen(
    favorites: List<Rule34Post>,
    imageLoader: ImageLoader,
    onOpenPost: (Rule34Post) -> Unit,
    onToggleFavorite: (Rule34Post) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(170.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CenterAlignedTopAppBar(title = { Text("Favorites") })
        }

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
                key = { it.id },
            ) { post ->
                PostCard(
                    post = post,
                    isFavorite = true,
                    imageLoader = imageLoader,
                    onOpenPost = onOpenPost,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}
