package com.perdonus.r34viewer.data.download

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.perdonus.r34viewer.data.model.Rule34Post

object MediaDownloadManager {
    fun requiresLegacyWritePermission(): Boolean = Build.VERSION.SDK_INT <= 28

    fun hasLegacyWritePermission(context: Context): Boolean {
        return !requiresLegacyWritePermission() ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun enqueue(
        context: Context,
        post: Rule34Post,
    ): Long? {
        val service = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return null
        return runCatching {
            val fileName = buildFileName(post)
            val request = DownloadManager.Request(android.net.Uri.parse(post.fileUrl))
                .setTitle(fileName)
                .setDescription("Скачивание в Downloads/rule")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "rule/$fileName")
                .setMimeType(resolveMimeType(post.fileUrl))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            service.enqueue(request)
        }.getOrNull()
    }

    private fun buildFileName(post: Rule34Post): String {
        val extension = post.fileUrl.substringBefore('?')
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.isNotBlank() }
            ?: if (post.isVideo) "mp4" else "jpg"
        return "${post.service.id}_${post.id}.$extension"
    }

    private fun resolveMimeType(url: String): String? {
        val extension = url.substringBefore('?')
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.isNotBlank() }
            ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
