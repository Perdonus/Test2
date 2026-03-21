package com.perdonus.r34viewer.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.perdonus.r34viewer.data.cache.VideoPlaybackCache
import com.perdonus.r34viewer.data.download.MediaDownloadManager
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.ui.components.EmbeddedWebVideoPlayer
import com.perdonus.r34viewer.ui.components.FullscreenVideoOverlay
import com.perdonus.r34viewer.ui.components.FullscreenWebVideoOverlay
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
    var playbackPosition by rememberSaveable(post.playbackUrl) { mutableLongStateOf(0L) }
    var playWhenReady by rememberSaveable(post.playbackUrl) { mutableStateOf(false) }

    val context = LocalContext.current
    val hasDirectVideoPlayback = post.isVideo && post.hasDirectMedia && post.service.supportsDirectMediaPlayback
    val startDownload = remember(post, context) {
        {
            if (!post.canDownloadDirectly) {
                Toast.makeText(
                    context,
                    "У этого сервиса нет прямой ссылки на файл для скачивания.",
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                val downloadId = MediaDownloadManager.enqueue(context, post)
                Toast.makeText(
                    context,
                    if (downloadId != null) {
                        "Скачивание началось: Downloads/rule"
                    } else {
                        "Не удалось начать скачивание"
                    },
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
    val downloadPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startDownload()
        } else {
            Toast.makeText(
                context,
                "Для Downloads/rule на Android 9 нужен доступ к памяти.",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val player = remember(post.playbackUrl, okHttpClient, hasDirectVideoPlayback) {
        if (!hasDirectVideoPlayback) return@remember null
        val mediaSourceFactory = DefaultMediaSourceFactory(
            VideoPlaybackCache.dataSourceFactory(okHttpClient),
        )
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(post.playbackUrl))
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
                    if (post.canDownloadDirectly) {
                        IconButton(
                            onClick = {
                                if (
                                    MediaDownloadManager.requiresLegacyWritePermission() &&
                                    !MediaDownloadManager.hasLegacyWritePermission(context)
                                ) {
                                    downloadPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    startDownload()
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = "Скачать в Downloads/rule",
                            )
                        }
                    }
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
                if (post.isVideo && hasDirectVideoPlayback && player != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(260.dp),
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (showVideoFullscreen) 0f else 1f),
                            factory = {
                                PlayerView(it).apply {
                                    useController = true
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShowPreviousButton(false)
                                    setShowNextButton(false)
                                }
                            },
                            update = { playerView ->
                                playerView.useController = true
                                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                playerView.setShowPreviousButton(false)
                                playerView.setShowNextButton(false)
                                playerView.player = if (showVideoFullscreen) null else player
                            },
                        )

                        if (!showVideoFullscreen) {
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
                } else if (post.isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(260.dp),
                    ) {
                        EmbeddedWebVideoPlayer(
                            url = post.playbackUrl,
                            modifier = Modifier.fillMaxSize(),
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
                            if (post.title.isNotBlank()) {
                                Text(post.title, style = MaterialTheme.typography.bodyMedium)
                            }
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
        } else if (showVideoFullscreen && post.isVideo) {
            FullscreenWebVideoOverlay(
                url = post.playbackUrl,
                onDismiss = { showVideoFullscreen = false },
            )
        }
    }
}
