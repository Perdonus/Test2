package com.perdonus.r34viewer.data.cache

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.io.File
import okhttp3.OkHttpClient

object VideoPlaybackCache {
    private val lock = Any()

    @Volatile
    private var rootDir: File? = null

    @Volatile
    private var cache: SimpleCache? = null

    fun initialize(context: Context) {
        val directory = File(context.cacheDir, "rule-video-cache").apply { mkdirs() }
        synchronized(lock) {
            rootDir = directory
            if (cache == null) {
                cache = createCache(directory)
            }
        }
    }

    fun dataSourceFactory(okHttpClient: OkHttpClient): DataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(requireCache())
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun sizeBytes(): Long {
        return synchronized(lock) {
            cache?.cacheSpace ?: 0L
        }
    }

    fun clear() {
        synchronized(lock) {
            val directory = rootDir ?: return
            cache?.release()
            cache = null
            directory.deleteRecursively()
            directory.mkdirs()
            cache = createCache(directory)
        }
    }

    private fun requireCache(): SimpleCache {
        return synchronized(lock) {
            val directory = rootDir ?: error("VideoPlaybackCache is not initialized.")
            cache ?: createCache(directory).also { cache = it }
        }
    }

    @Suppress("DEPRECATION")
    private fun createCache(directory: File): SimpleCache {
        return SimpleCache(directory, NoOpCacheEvictor())
    }
}
