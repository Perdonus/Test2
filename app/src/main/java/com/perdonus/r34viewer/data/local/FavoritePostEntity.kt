package com.perdonus.r34viewer.data.local

import androidx.room.Entity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.PostMediaType
import com.perdonus.r34viewer.data.model.Rule34Post

@Entity(
    tableName = "favorite_posts",
    primaryKeys = ["serviceId", "id"],
)
data class FavoritePostEntity(
    val serviceId: String,
    val id: String,
    val previewUrl: String?,
    val sampleUrl: String?,
    val fileUrl: String,
    val pageUrl: String?,
    val embedUrl: String?,
    val tagsRaw: String,
    val rating: String,
    val score: Int,
    val width: Int,
    val height: Int,
    val mediaType: String,
    val hasDirectMedia: Boolean,
    val savedAt: Long,
)

fun FavoritePostEntity.toDomain(): Rule34Post = Rule34Post(
    service = BooruService.fromId(serviceId),
    id = id,
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    pageUrl = pageUrl,
    embedUrl = embedUrl,
    tags = tagsRaw.split(" ").filter { it.isNotBlank() },
    rating = rating,
    score = score,
    width = width,
    height = height,
    mediaType = runCatching { PostMediaType.valueOf(mediaType) }.getOrDefault(PostMediaType.UNKNOWN),
    hasDirectMedia = hasDirectMedia,
)

fun Rule34Post.toEntity(savedAt: Long = System.currentTimeMillis()): FavoritePostEntity = FavoritePostEntity(
    serviceId = service.id,
    id = id,
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    pageUrl = pageUrl,
    embedUrl = embedUrl,
    tagsRaw = tags.joinToString(" "),
    rating = rating,
    score = score,
    width = width,
    height = height,
    mediaType = mediaType.name,
    hasDirectMedia = hasDirectMedia,
    savedAt = savedAt,
)
