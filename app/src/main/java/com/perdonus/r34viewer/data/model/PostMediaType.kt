package com.perdonus.r34viewer.data.model

enum class PostMediaType {
    IMAGE,
    VIDEO,
    UNKNOWN,
    ;

    companion object {
        fun fromUrl(url: String?): PostMediaType {
            val extension = url
                ?.substringAfterLast('.', "")
                ?.substringBefore('?')
                ?.lowercase()
                .orEmpty()

            return when (extension) {
                "webm", "mp4", "m4v", "mov" -> VIDEO
                "jpg", "jpeg", "png", "gif", "webp" -> IMAGE
                else -> UNKNOWN
            }
        }
    }
}
