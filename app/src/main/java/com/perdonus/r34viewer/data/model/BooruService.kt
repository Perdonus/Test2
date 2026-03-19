package com.perdonus.r34viewer.data.model

enum class BooruService(
    val id: String,
    val displayName: String,
    val supportsVideo: Boolean,
) {
    RULE34(
        id = "rule34",
        displayName = "Rule34",
        supportsVideo = true,
    ),
    KONACHAN(
        id = "konachan",
        displayName = "Konachan",
        supportsVideo = false,
    ),
    XBOORU(
        id = "xbooru",
        displayName = "xBooru",
        supportsVideo = true,
    ),
    ;

    companion object {
        fun fromId(id: String?): BooruService {
            return entries.firstOrNull { it.id == id } ?: RULE34
        }
    }
}
