package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.ui.components.FullscreenVideoOverlay
import com.perdonus.r34viewer.ui.components.RemoteImage
import com.perdonus.r34viewer.ui.components.ScreenHeader
import com.perdonus.r34viewer.ui.components.ZoomableImageOverlay
import okhttp3.OkHttpClient

@OptIn(
    ExperimentalLayoutApi::class,
    UnstableApi::class,
)
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
            ScreenHeader(
                title = "Пост",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
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

    var showImageFullscreen by rememberSaveable(post.serviceScopedId) { mutableStateOf(false) }
    var showVideoFullscreen by rememberSaveable(post.serviceScopedId) { mutableStateOf(false) }
    var playbackPosition by rememberSaveable(post.fileUrl) { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable(post.fileUrl) { mutableStateOf(false) }

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
                seekTo(playbackPosition)
                this.playWhenReady = playWhenReady
            }
    }

    DisposableEffect(player) {
        onDispose {
            playbackPosition = player?.currentPosition ?: playbackPosition
            playWhenReady = player?.playWhenReady ?: playWhenReady
            player?.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "${post.service.displayName} #${post.id}",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { onToggleFavorite(post) }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Убрать из избранного" else "Добавить в избранное",
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (post.isVideo && player != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(260.dp),
                    ) {
                        if (!showVideoFullscreen) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = {
                                    PlayerView(it).apply {
                                        this.player = player
                                        useController = true
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = player
                                },
                            )

                            IconButton(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp),
                                onClick = { showVideoFullscreen = true },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Fullscreen,
                                    contentDescription = "Открыть видео во весь экран",
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showImageFullscreen = true },
                    ) {
                        RemoteImage(
                            modifier = Modifier.fillMaxWidth(),
                            url = post.detailImageUrl,
                            okHttpClient = okHttpClient,
                            contentDescription = "Пост ${post.id}",
                            contentScale = ContentScale.FillWidth,
                            maxDecodeDimensionPx = 1600,
                        )
                    }
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
                            Text("Информация", style = MaterialTheme.typography.titleMedium)
                            Text("Тип: ${if (post.isVideo) "Видео" else "Изображение"}")
                            Text("Сервис: ${post.service.displayName}")
                            Text("Рейтинг: ${post.rating.uppercase()}")
                            Text("Счёт: ${post.score}")
                            Text("Размер: ${post.width} x ${post.height}")
                        }
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Теги", style = MaterialTheme.typography.titleMedium)
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

        if (showImageFullscreen) {
            ZoomableImageOverlay(
                url = post.detailImageUrl,
                okHttpClient = okHttpClient,
                contentDescription = "Пост ${post.id}",
                imageWidth = post.width,
                imageHeight = post.height,
                onDismiss = { showImageFullscreen = false },
            )
        }

        if (showVideoFullscreen && player != null) {
            FullscreenVideoOverlay(
                player = player,
                onDismiss = { showVideoFullscreen = false },
            )
        }
    }
}
