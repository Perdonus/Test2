package com.perdonus.r34viewer.data.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

private val VideoExtensions = setOf("mp4", "webm", "m4v", "mov", "mkv")

object MediaDiskCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var rootDir: File? = null

    fun initialize(context: Context) {
        rootDir = File(context.cacheDir, "rule-media-cache").apply { mkdirs() }
    }

    fun cachedFile(url: String): File? {
        val file = resolveFile(url)
        return file.takeIf { it.exists() && it.isFile && it.length() > 0L }
    }

    fun prefetch(
        url: String,
        okHttpClient: OkHttpClient,
    ) {
        if (cachedFile(url) != null) return
        scope.launch {
            getOrFetch(url, okHttpClient)
        }
    }

    fun getOrFetch(
        url: String,
        okHttpClient: OkHttpClient,
    ): File? {
        cachedFile(url)?.let { return it }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            val targetFile = resolveFile(url)
            val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (tempFile.length() <= 0L) {
                tempFile.delete()
                return null
            }

            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) {
                tempFile.delete()
                return targetFile
            }

            if (!tempFile.renameTo(targetFile)) {
                targetFile.outputStream().use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                tempFile.delete()
            }

            return targetFile
        }
    }

    fun sizeBytes(): Long {
        val directory = rootDir ?: return 0L
        if (!directory.exists()) return 0L
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun clear() {
        val directory = rootDir ?: return
        directory.deleteRecursively()
        directory.mkdirs()
    }

    private fun resolveFile(url: String): File {
        val directory = rootDir ?: error("MediaDiskCache is not initialized.")
        val extension = guessExtension(url)
        return File(directory, "${sha256(url)}.$extension")
    }

    private fun guessExtension(url: String): String {
        val rawPath = url.substringBefore('?').substringAfterLast('/', "")
        val fromUrl = rawPath.substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.length in 2..5 && it.all(Char::isLetterOrDigit) }
        if (fromUrl != null) return fromUrl
        return if (looksLikeVideo(url)) "mp4" else "img"
    }

    private fun looksLikeVideo(url: String): Boolean {
        val extension = url.substringBefore('?')
            .substringAfterLast('.', "")
            .lowercase()
        return extension in VideoExtensions
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                append(((byte.toInt() shr 4) and 0xF).toString(16))
                append((byte.toInt() and 0xF).toString(16))
            }
        }
    }
}
