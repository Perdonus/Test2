package com.perdonus.r34viewer.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun RemoteImage(
    url: String,
    okHttpClient: OkHttpClient,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = BitmapMemoryCache.get(url), url, okHttpClient) {
        if (value != null) return@produceState
        value = fetchBitmap(url, okHttpClient)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}

private suspend fun fetchBitmap(url: String, okHttpClient: OkHttpClient): Bitmap? = withContext(Dispatchers.IO) {
    BitmapMemoryCache.get(url)?.let { return@withContext it }

    val request = Request.Builder()
        .url(url)
        .get()
        .build()

    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val bitmap = response.body?.byteStream()?.use(BitmapFactory::decodeStream) ?: return@withContext null
        BitmapMemoryCache.put(url, bitmap)
        return@withContext bitmap
    }
}

private object BitmapMemoryCache {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun get(url: String): Bitmap? = cache.get(url)

    fun put(url: String, bitmap: Bitmap) {
        cache.put(url, bitmap)
    }
}
