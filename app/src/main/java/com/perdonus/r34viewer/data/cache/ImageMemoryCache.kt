package com.perdonus.r34viewer.data.cache

import android.graphics.Bitmap
import android.util.LruCache

private const val CacheSeparator = "|"
private const val MinCacheSizeKb = 64 * 1024
private const val MaxCacheSizeKb = 256 * 1024

object ImageMemoryCache {
    private val maxSizeKb = ((Runtime.getRuntime().maxMemory() / 8L) / 1024L)
        .coerceIn(MinCacheSizeKb.toLong(), MaxCacheSizeKb.toLong())
        .toInt()

    private val cache = object : LruCache<String, Bitmap>(maxSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024
        }
    }

    fun get(
        url: String,
        maxDecodeDimensionPx: Int,
    ): Bitmap? {
        cache.get(makeKey(url, maxDecodeDimensionPx))?.let { return it }

        val bestFallback = cache.snapshot()
            .asSequence()
            .filter { (key, _) -> key.startsWith("$url$CacheSeparator") }
            .mapNotNull { (key, bitmap) ->
                key.substringAfter(CacheSeparator, "")
                    .toIntOrNull()
                    ?.let { dimension -> dimension to bitmap }
            }
            .maxByOrNull { (dimension, _) -> dimension }

        return bestFallback?.second
    }

    fun put(
        url: String,
        maxDecodeDimensionPx: Int,
        bitmap: Bitmap,
    ) {
        cache.put(makeKey(url, maxDecodeDimensionPx), bitmap)
    }

    fun sizeBytes(): Long = cache.size().toLong() * 1024L

    fun maxSizeBytes(): Long = cache.maxSize().toLong() * 1024L

    fun clear() {
        cache.evictAll()
    }

    private fun makeKey(
        url: String,
        maxDecodeDimensionPx: Int,
    ): String = "$url$CacheSeparator$maxDecodeDimensionPx"
}
