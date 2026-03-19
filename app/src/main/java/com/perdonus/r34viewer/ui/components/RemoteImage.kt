package com.perdonus.r34viewer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.data.cache.ImageMemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private sealed interface ImageLoadState {
    data object Loading : ImageLoadState
    data class Success(val bitmap: Bitmap) : ImageLoadState
    data object Error : ImageLoadState
}

@Composable
fun RemoteImage(
    url: String,
    okHttpClient: OkHttpClient,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxDecodeDimensionPx: Int = 1600,
) {
    val state by produceState<ImageLoadState>(
        initialValue = ImageMemoryCache.get(url, maxDecodeDimensionPx)?.let(ImageLoadState::Success) ?: ImageLoadState.Loading,
        key1 = url,
        key2 = maxDecodeDimensionPx,
        key3 = okHttpClient,
    ) {
        if (value is ImageLoadState.Success) return@produceState
        value = fetchBitmap(
            url = url,
            okHttpClient = okHttpClient,
            maxDecodeDimensionPx = maxDecodeDimensionPx,
        )
            ?.let(ImageLoadState::Success)
            ?: ImageLoadState.Error
    }

    when (val currentState = state) {
        is ImageLoadState.Success -> {
            Image(
                bitmap = currentState.bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale,
            )
        }

        ImageLoadState.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text("Не удалось загрузить изображение")
            }
        }

        ImageLoadState.Loading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}

private suspend fun fetchBitmap(
    url: String,
    okHttpClient: OkHttpClient,
    maxDecodeDimensionPx: Int,
): Bitmap? = withContext(Dispatchers.IO) {
    ImageMemoryCache.get(url, maxDecodeDimensionPx)?.let { return@withContext it }

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val bytes = response.body?.bytes() ?: return@withContext null
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inDither = true
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDecodeDimensionPx)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return@withContext null
        ImageMemoryCache.put(url, maxDecodeDimensionPx, bitmap)
        return@withContext bitmap
    }
}

private fun calculateSampleSize(
    width: Int,
    height: Int,
    maxDecodeDimensionPx: Int,
): Int {
    if (width <= 0 || height <= 0) return 1

    val maxDimension = maxOf(width, height)
    var sampleSize = 1
    while (maxDimension / sampleSize > maxDecodeDimensionPx) {
        sampleSize *= 2
    }
    return sampleSize
}
