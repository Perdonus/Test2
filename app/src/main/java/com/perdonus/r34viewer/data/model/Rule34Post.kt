package com.perdonus.r34viewer.data.model

private const val AI_GENERATED_TAG = "ai_generated"
private const val AI_ASSISTED_TAG = "ai_assisted"

data class Rule34Post(
    val service: BooruService,
    val id: String,
    val previewUrl: String?,
    val sampleUrl: String?,
    val fileUrl: String,
    val pageUrl: String? = null,
    val embedUrl: String? = null,
    val tags: List<String>,
    val rating: String,
    val score: Int,
    val width: Int,
    val height: Int,
    val mediaType: PostMediaType,
    val hasDirectMedia: Boolean = true,
) {
    val serviceScopedId: String
        get() = "${service.id}:$id"

    val thumbnailUrl: String
        get() = previewUrl ?: sampleUrl ?: pageUrl ?: embedUrl ?: fileUrl

    val detailImageUrl: String
        get() = sampleUrl ?: previewUrl ?: fileUrl

    val isVideo: Boolean
        get() = mediaType == PostMediaType.VIDEO

    val playbackUrl: String
        get() = if (hasDirectMedia) {
            fileUrl
        } else {
            embedUrl ?: pageUrl ?: fileUrl
        }

    val canDownloadDirectly: Boolean
        get() = hasDirectMedia && fileUrl.isNotBlank()

    val areTagsAiRelated: Boolean
        get() = tags.any { it == AI_GENERATED_TAG || it == AI_ASSISTED_TAG }

    val tagsSummary: String
        get() = tags.take(6).joinToString(" ")
}
