package com.perdonus.r34viewer.data.model

private const val AI_GENERATED_TAG = "ai_generated"
private const val AI_ASSISTED_TAG = "ai_assisted"

data class Rule34Post(
    val id: Int,
    val previewUrl: String?,
    val sampleUrl: String?,
    val fileUrl: String,
    val tags: List<String>,
    val rating: String,
    val score: Int,
    val width: Int,
    val height: Int,
    val mediaType: PostMediaType,
) {
    val thumbnailUrl: String
        get() = previewUrl ?: sampleUrl ?: fileUrl

    val isVideo: Boolean
        get() = mediaType == PostMediaType.VIDEO

    val areTagsAiRelated: Boolean
        get() = tags.any { it == AI_GENERATED_TAG || it == AI_ASSISTED_TAG }

    val tagsSummary: String
        get() = tags.take(6).joinToString(" ")
}
