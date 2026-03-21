package com.perdonus.r34viewer.data.model

enum class SearchSyntax {
    TAGS,
    TEXT,
}

enum class PlaybackMode {
    DIRECT_MEDIA,
    EMBEDDED_PAGE,
}

enum class BooruService(
    val id: String,
    val displayName: String,
    val supportsVideo: Boolean,
    val searchSyntax: SearchSyntax,
    val playbackMode: PlaybackMode,
    val isSelectableInClient: Boolean = true,
) {
    RULE34(
        id = "rule34",
        displayName = "Rule34",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TAGS,
        playbackMode = PlaybackMode.DIRECT_MEDIA,
    ),
    PORNHUB(
        id = "pornhub",
        displayName = "Pornhub",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TEXT,
        playbackMode = PlaybackMode.EMBEDDED_PAGE,
    ),
    KONACHAN(
        id = "konachan",
        displayName = "Konachan",
        supportsVideo = false,
        searchSyntax = SearchSyntax.TAGS,
        playbackMode = PlaybackMode.DIRECT_MEDIA,
    ),
    XBOORU(
        id = "xbooru",
        displayName = "xBooru",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TAGS,
        playbackMode = PlaybackMode.DIRECT_MEDIA,
    ),
    TBIB(
        id = "tbib",
        displayName = "TBIB",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TAGS,
        playbackMode = PlaybackMode.DIRECT_MEDIA,
        isSelectableInClient = false,
    ),
    EPORNER(
        id = "eporner",
        displayName = "Eporner",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TEXT,
        playbackMode = PlaybackMode.EMBEDDED_PAGE,
        isSelectableInClient = false,
    ),
    REDTUBE(
        id = "redtube",
        displayName = "RedTube",
        supportsVideo = true,
        searchSyntax = SearchSyntax.TEXT,
        playbackMode = PlaybackMode.EMBEDDED_PAGE,
        isSelectableInClient = false,
    ),
    ;

    val usesTagSearch: Boolean
        get() = searchSyntax == SearchSyntax.TAGS

    val supportsDirectMediaPlayback: Boolean
        get() = playbackMode == PlaybackMode.DIRECT_MEDIA

    companion object {
        fun fromId(id: String?): BooruService {
            return entries.firstOrNull { it.id == id } ?: RULE34
        }
    }

    fun asSelectableOrDefault(): BooruService {
        return if (isSelectableInClient) this else RULE34
    }
}
