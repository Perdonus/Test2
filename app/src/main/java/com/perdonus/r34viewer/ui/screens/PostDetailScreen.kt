package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.ui.components.RemoteImage
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalLayoutApi::class)
@OptIn(UnstableApi::class)
@Composable
fun PostDetailScreen(
    post: Rule34Post?,
    isFavorite: Boolean,
    okHttpClient: OkHttpClient,
    onBack: () -> Unit,
    onToggleFavorite: (Rule34Post) -> Unit,
    onTagSelected: (String) -> Unit,
) {
    if (post == null) {
        Column(modifier = Modifier.fillMaxSize()) {
            CenterAlignedTopAppBar(
                title = { Text("Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyState(
                    title = "Пост не выбран",
                    subtitle = "Вернитесь в поиск или откройте пост из избранного.",
                )
            }
        }
        return
    }

    val context = LocalContext.current
    val player = remember(post.fileUrl, okHttpClient) {
        if (!post.isVideo) return@remember null
        val mediaSourceFactory = DefaultMediaSourceFactory(
            OkHttpDataSource.Factory(okHttpClient),
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(post.fileUrl))
                prepare()
            }
    }

    DisposableEffect(player) {
        onDispose {
            player?.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CenterAlignedTopAppBar(
            title = { Text("#${post.id}") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { onToggleFavorite(post) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                    )
                }
            },
        )

        if (post.isVideo && player != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = 16.dp),
                factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                    }
                },
            )
        } else {
            RemoteImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                url = post.fileUrl,
                okHttpClient = okHttpClient,
                contentDescription = "Post ${post.id}",
                contentScale = ContentScale.FillWidth,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Media info", style = MaterialTheme.typography.titleMedium)
                    Text("Type: ${if (post.isVideo) "Video" else "Image"}")
                    Text("Rating: ${post.rating.uppercase()}")
                    Text("Score: ${post.score}")
                    Text("Size: ${post.width} x ${post.height}")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        post.tags.forEach { tag ->
                            AssistChip(
                                onClick = { onTagSelected(tag) },
                                label = { Text(tag) },
                            )
                        }
                    }
                }
            }
        }
    }
}
