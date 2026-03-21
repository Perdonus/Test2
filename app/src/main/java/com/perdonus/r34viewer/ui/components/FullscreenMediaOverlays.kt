package com.perdonus.r34viewer.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import okhttp3.OkHttpClient
import kotlin.math.max

private const val MinImageScale = 1f
private const val MaxImageScale = 5f

@Composable
fun FullscreenVideoOverlay(
    player: Player,
    onDismiss: () -> Unit,
) {
    FullscreenMediaDialog(
        onDismiss = onDismiss,
        rotateWithSensor = true,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PlayerView(context).applyPlayerChrome()
            },
            update = { playerView ->
                playerView.player = player
                playerView.applyPlayerChrome()
            },
        )
    }
}

@Composable
fun EmbeddedWebVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).applyVideoWebView(url)
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
    )
}

@Composable
fun FullscreenWebVideoOverlay(
    url: String,
    onDismiss: () -> Unit,
) {
    FullscreenMediaDialog(
        onDismiss = onDismiss,
        rotateWithSensor = true,
    ) {
        EmbeddedWebVideoPlayer(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun ZoomableImageOverlay(
    url: String,
    okHttpClient: OkHttpClient,
    contentDescription: String?,
    imageWidth: Int,
    imageHeight: Int,
    onDismiss: () -> Unit,
) {
    var scale by rememberSaveable(url) { mutableFloatStateOf(MinImageScale) }
    var offsetX by rememberSaveable(url) { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable(url) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    FullscreenMediaDialog(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .onSizeChanged { viewportSize = it }
                .pointerInput(url, viewportSize, imageWidth, imageHeight) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val updatedScale = (scale * zoom).coerceIn(MinImageScale, MaxImageScale)
                        val unclampedOffset = if (updatedScale == MinImageScale) {
                            Offset.Zero
                        } else {
                            Offset(offsetX + pan.x, offsetY + pan.y)
                        }
                        val clampedOffset = clampImageOffset(
                            rawOffset = unclampedOffset,
                            scale = updatedScale,
                            viewportSize = viewportSize,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                        )

                        scale = updatedScale
                        offsetX = clampedOffset.x
                        offsetY = clampedOffset.y
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            RemoteImage(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    },
                url = url,
                okHttpClient = okHttpClient,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                maxDecodeDimensionPx = 1600,
            )
        }
    }
}

@Composable
private fun FullscreenMediaDialog(
    onDismiss: () -> Unit,
    rotateWithSensor: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)
        FullscreenWindowEffect(rotateWithSensor = rotateWithSensor)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
        ) {
            content()

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
                onClick = onDismiss,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun FullscreenWindowEffect(rotateWithSensor: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(context, view, rotateWithSensor) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        if (rotateWithSensor) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (rotateWithSensor) {
                activity?.requestedOrientation = previousOrientation
            }
        }
    }
}

private fun PlayerView.applyPlayerChrome(): PlayerView = apply {
    useController = true
    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    setShowPreviousButton(false)
    setShowNextButton(false)
}

private fun WebView.applyVideoWebView(url: String): WebView = apply {
    setBackgroundColor(AndroidColor.BLACK)
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(this, true)
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.loadsImagesAutomatically = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.allowContentAccess = true
    settings.allowFileAccess = false
    settings.userAgentString =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    settings.javaScriptCanOpenWindowsAutomatically = true
    settings.setSupportMultipleWindows(false)
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            url: String?,
        ): Boolean = false
    }
    webChromeClient = WebChromeClient()
    loadUrl(url)
}

private fun clampImageOffset(
    rawOffset: Offset,
    scale: Float,
    viewportSize: IntSize,
    imageWidth: Int,
    imageHeight: Int,
): Offset {
    if (viewportSize == IntSize.Zero) {
        return Offset.Zero
    }

    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()
    val safeImageWidth = imageWidth.coerceAtLeast(1).toFloat()
    val safeImageHeight = imageHeight.coerceAtLeast(1).toFloat()
    val imageAspectRatio = safeImageWidth / safeImageHeight
    val viewportAspectRatio = viewportWidth / viewportHeight

    val fittedWidth = if (imageAspectRatio > viewportAspectRatio) {
        viewportWidth
    } else {
        viewportHeight * imageAspectRatio
    }
    val fittedHeight = if (imageAspectRatio > viewportAspectRatio) {
        viewportWidth / imageAspectRatio
    } else {
        viewportHeight
    }

    val maxOffsetX = max(0f, ((fittedWidth * scale) - viewportWidth) / 2f)
    val maxOffsetY = max(0f, ((fittedHeight * scale) - viewportHeight) / 2f)

    return Offset(
        x = rawOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
        y = rawOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
