package com.perdonus.r34viewer.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.PostMediaType
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.model.SearchQueryBuilder
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.data.settings.ContentPreferences
import com.perdonus.r34viewer.data.settings.ServiceApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class BooruApiException(message: String) : Exception(message)

class BooruApiSource(
    private val networkClientFactory: NetworkClientFactory,
) {
    private val browserUserAgent =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun searchPosts(
        settings: AppSettings,
        rawQuery: String,
        page: Int,
        limit: Int,
    ): List<Rule34Post> = withContext(Dispatchers.IO) {
        val service = settings.selectedService
        val query = SearchQueryBuilder.build(
            service = service,
            query = rawQuery,
            hideAiContent = settings.hideAiContent,
            preferences = settings.preferences,
        )
        validateServiceApiConfig(service, settings.serviceApiConfig)
        val client = networkClientFactory.create(settings)
        val request = Request.Builder()
            .url(buildUrl(service, query, page, limit, settings.serviceApiConfig))
            .get()
            .applyServiceHeaders(service)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw BooruApiException("${service.displayName}: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty().trim()
            if (body.isEmpty()) return@withContext emptyList()
            if (body.startsWith("<!DOCTYPE html") || body.startsWith("<html")) {
                throw BooruApiException(
                    when (service) {
                        BooruService.TBIB -> "TBIB сейчас закрывает API Cloudflare-защитой."
                        else -> "${service.displayName} вернул HTML вместо API-ответа."
                    },
                )
            }

            val element = json.parseToJsonElement(body)
            return@withContext parsePosts(
                service = service,
                element = element,
                preferences = settings.preferences,
                hideAiContent = settings.hideAiContent,
            )
        }
    }

    private fun validateServiceApiConfig(
        service: BooruService,
        serviceApiConfig: ServiceApiConfig,
    ) {
        if (
            service == BooruService.RULE34 &&
            (serviceApiConfig.rule34.userId.isBlank() || serviceApiConfig.rule34.apiKey.isBlank())
        ) {
            throw BooruApiException("Rule34 API на сервере ещё не настроен.")
        }
    }

    private fun buildUrl(
        service: BooruService,
        query: String,
        page: Int,
        limit: Int,
        serviceApiConfig: ServiceApiConfig,
    ) = when (service) {
        BooruService.RULE34 -> {
            if (serviceApiConfig.rule34.userId.isBlank() || serviceApiConfig.rule34.apiKey.isBlank()) {
                throw BooruApiException("Rule34 API не настроен на сервере.")
            }
            "https://api.rule34.xxx/index.php".toHttpUrl().newBuilder()
                .addQueryParameter("page", "dapi")
                .addQueryParameter("s", "post")
                .addQueryParameter("q", "index")
                .addQueryParameter("json", "1")
                .addQueryParameter("user_id", serviceApiConfig.rule34.userId)
                .addQueryParameter("api_key", serviceApiConfig.rule34.apiKey)
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("pid", page.toString())
                .addOptionalTags(query)
                .build()
        }

        BooruService.KONACHAN -> "https://konachan.com/post.json".toHttpUrl().newBuilder()
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("page", (page + 1).toString())
            .addOptionalQueryParameter("api_key", serviceApiConfig.konachan.apiKey)
            .addOptionalTags(query)
            .build()

        BooruService.XBOORU -> "https://xbooru.com/index.php".toHttpUrl().newBuilder()
            .addQueryParameter("page", "dapi")
            .addQueryParameter("s", "post")
            .addQueryParameter("q", "index")
            .addQueryParameter("json", "1")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("pid", page.toString())
            .addOptionalTags(query)
            .build()

        BooruService.TBIB -> "https://tbib.org/index.php".toHttpUrl().newBuilder()
            .addQueryParameter("page", "dapi")
            .addQueryParameter("s", "post")
            .addQueryParameter("q", "index")
            .addQueryParameter("json", "1")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("pid", page.toString())
            .addOptionalTags(query)
            .build()

        BooruService.PORNHUB -> "https://rt.pornhub.org/webmasters/search".toHttpUrl().newBuilder()
            .addQueryParameter("page", (page + 1).toString())
            .addQueryParameter("thumbsize", "medium")
            .addOptionalQueryParameter("search", query)
            .build()

        BooruService.REDTUBE -> "https://api.redtube.com/".toHttpUrl().newBuilder()
            .addQueryParameter("data", "redtube.Videos.searchVideos")
            .addQueryParameter("output", "json")
            .addQueryParameter("page", (page + 1).toString())
            .addOptionalQueryParameter("search", query)
            .build()

        BooruService.EPORNER -> "https://www.eporner.com/api/v2/video/search/".toHttpUrl().newBuilder()
            .addQueryParameter("query", query.ifBlank { "all" })
            .addQueryParameter("per_page", limit.toString())
            .addQueryParameter("page", (page + 1).toString())
            .addQueryParameter("thumbsize", "big")
            .addQueryParameter("order", "latest")
            .addQueryParameter("format", "json")
            .build()
    }

    private fun okhttp3.HttpUrl.Builder.addOptionalTags(query: String): okhttp3.HttpUrl.Builder {
        return if (query.isBlank()) this else addQueryParameter("tags", query)
    }

    private fun okhttp3.HttpUrl.Builder.addOptionalQueryParameter(
        name: String,
        value: String,
    ): okhttp3.HttpUrl.Builder {
        return if (value.isBlank()) this else addQueryParameter(name, value)
    }

    private fun Request.Builder.applyServiceHeaders(service: BooruService): Request.Builder = apply {
        when (service) {
            BooruService.PORNHUB -> {
                header("User-Agent", browserUserAgent)
                header("Accept", "application/json")
                header("Referer", "https://rt.pornhub.org/")
            }

            BooruService.REDTUBE -> {
                header("User-Agent", browserUserAgent)
                header("Accept", "application/json")
                header("Referer", "https://www.redtube.com/")
            }

            BooruService.EPORNER -> {
                header("User-Agent", browserUserAgent)
                header("Accept", "application/json")
                header("Referer", "https://www.eporner.com/")
            }

            BooruService.TBIB -> {
                header("User-Agent", browserUserAgent)
                header("Accept", "application/json,text/plain,*/*")
                header("Referer", "https://tbib.org/")
            }

            else -> Unit
        }
    }

    private fun parsePosts(
        service: BooruService,
        element: JsonElement,
        preferences: ContentPreferences,
        hideAiContent: Boolean,
    ): List<Rule34Post> {
        val posts = when (service) {
            BooruService.RULE34,
            BooruService.KONACHAN,
            BooruService.XBOORU,
            BooruService.TBIB,
            -> (element as? JsonArray).orEmptyArray().mapNotNull { it.asBooruPostOrNull(service) }

            BooruService.PORNHUB -> (element as? JsonObject)
                ?.get("videos")
                .orEmptyArray()
                .mapNotNull { it.asPornhubPostOrNull() }

            BooruService.REDTUBE -> (element as? JsonObject)
                ?.get("videos")
                .orEmptyArray()
                .mapNotNull { it.asRedTubePostOrNull() }

            BooruService.EPORNER -> (element as? JsonObject)
                ?.get("videos")
                .orEmptyArray()
                .mapNotNull { it.asEpornerPostOrNull() }
        }
        val blockedTags = buildBlockedTags(preferences)

        return posts.filter { post ->
            if (hideAiContent && post.areTagsAiRelated) {
                return@filter false
            }
            blockedTags.none { it in post.normalizedTags }
        }
    }

    private fun JsonElement.asBooruPostOrNull(
        service: BooruService,
    ): Rule34Post? {
        val content = this as? JsonObject ?: return null
        val fileUrl = content["file_url"].primitiveContentOrNull().orEmpty()
        if (fileUrl.isBlank()) return null
        val id = content["id"].primitiveContentOrNull().orEmpty()
        if (id.isBlank()) return null

        val tags = content["tags"]
            .primitiveContentOrNull()
            .orEmpty()
            .split(" ")
            .filter { it.isNotBlank() }

        return Rule34Post(
            service = service,
            id = id,
            title = "",
            previewUrl = content["preview_url"].primitiveContentOrNull(),
            sampleUrl = content["sample_url"].primitiveContentOrNull(),
            fileUrl = fileUrl,
            pageUrl = content["source"].primitiveContentOrNull(),
            tags = tags,
            rating = content["rating"].primitiveContentOrNull().orEmpty(),
            score = content["score"].primitiveIntOrZero(),
            width = content["width"].primitiveIntOrZero(),
            height = content["height"].primitiveIntOrZero(),
            mediaType = PostMediaType.fromUrl(fileUrl),
            hasDirectMedia = true,
        )
    }

    private fun JsonElement.asPornhubPostOrNull(): Rule34Post? {
        val content = this as? JsonObject ?: return null
        val id = content["video_id"].primitiveContentOrNull().orEmpty()
        val pageUrl = content["url"].primitiveContentOrNull().orEmpty()
        val previewUrl = content["thumb"].primitiveContentOrNull()
        val defaultThumb = content["default_thumb"].primitiveContentOrNull()
        if (id.isBlank() || pageUrl.isBlank()) return null
        val normalizedPageUrl = pageUrl.replace("www.pornhub.com", "rt.pornhub.org")
        return Rule34Post(
            service = BooruService.PORNHUB,
            id = id,
            title = content["title"].primitiveContentOrNull().orEmpty(),
            previewUrl = previewUrl ?: defaultThumb,
            sampleUrl = defaultThumb ?: previewUrl,
            fileUrl = normalizedPageUrl,
            pageUrl = normalizedPageUrl,
            embedUrl = "https://www.pornhub.com/embed/$id",
            tags = buildList {
                addAll(content["tags"].objectArrayTags("tag_name"))
                addAll(content["categories"].objectArrayTags("category"))
                addAll(content["pornstars"].objectArrayTags("pornstar_name"))
            }.distinct(),
            rating = "adult",
            score = content["views"].primitiveIntOrZero(),
            width = content["thumbs"].firstObjectOrNull()?.get("width").primitiveIntOrZero(),
            height = content["thumbs"].firstObjectOrNull()?.get("height").primitiveIntOrZero(),
            mediaType = PostMediaType.VIDEO,
            hasDirectMedia = false,
        )
    }

    private fun JsonElement.asRedTubePostOrNull(): Rule34Post? {
        val wrapper = this as? JsonObject ?: return null
        val content = wrapper["video"] as? JsonObject ?: wrapper
        val id = content["video_id"].primitiveContentOrNull().orEmpty()
        val pageUrl = content["url"].primitiveContentOrNull().orEmpty()
        val embedUrl = content["embed_url"].primitiveContentOrNull()
        val previewUrl = content["thumb"].primitiveContentOrNull()
        val defaultThumb = content["default_thumb"].primitiveContentOrNull()
        if (id.isBlank() || pageUrl.isBlank()) return null
        return Rule34Post(
            service = BooruService.REDTUBE,
            id = id,
            title = content["title"].primitiveContentOrNull().orEmpty(),
            previewUrl = previewUrl ?: defaultThumb,
            sampleUrl = defaultThumb ?: previewUrl,
            fileUrl = pageUrl,
            pageUrl = pageUrl,
            embedUrl = embedUrl?.takeIf { it.startsWith("http") } ?: "https://embed.redtube.com/?id=$id",
            tags = buildList {
                addAll(content["tags"].objectArrayTags("tag_name"))
                addAll(content["categories"].objectArrayTags("category"))
                addAll(content["pornstars"].objectArrayTags("pornstar_name"))
            }.distinct(),
            rating = "adult",
            score = content["views"].primitiveIntOrZero(),
            width = content["thumbs"].firstObjectOrNull()?.get("width").primitiveIntOrZero(),
            height = content["thumbs"].firstObjectOrNull()?.get("height").primitiveIntOrZero(),
            mediaType = PostMediaType.VIDEO,
            hasDirectMedia = false,
        )
    }

    private fun JsonElement.asEpornerPostOrNull(): Rule34Post? {
        val content = this as? JsonObject ?: return null
        val id = content["id"].primitiveContentOrNull().orEmpty()
        val pageUrl = content["url"].primitiveContentOrNull().orEmpty()
        if (id.isBlank() || pageUrl.isBlank()) return null
        val defaultThumb = content["default_thumb"] as? JsonObject
        val previewUrl = defaultThumb?.get("src").primitiveContentOrNull()
            ?: (content["thumbs"] as? JsonArray)
                ?.firstOrNull()
                ?.let { it as? JsonObject }
                ?.get("src")
                .primitiveContentOrNull()
        val embedRaw = content["embed"].primitiveContentOrNull().orEmpty()
        val embedUrl = extractEmbedSrc(embedRaw) ?: "https://www.eporner.com/embed/$id"
        return Rule34Post(
            service = BooruService.EPORNER,
            id = id,
            title = content["title"].primitiveContentOrNull().orEmpty(),
            previewUrl = previewUrl,
            sampleUrl = previewUrl,
            fileUrl = pageUrl,
            pageUrl = pageUrl,
            embedUrl = embedUrl,
            tags = parseDelimitedTags(content["keywords"].primitiveContentOrNull()),
            rating = "adult",
            score = content["views"].primitiveIntOrZero(),
            width = defaultThumb?.get("width").primitiveIntOrZero(),
            height = defaultThumb?.get("height").primitiveIntOrZero(),
            mediaType = PostMediaType.VIDEO,
            hasDirectMedia = false,
        )
    }

    private fun buildBlockedTags(preferences: ContentPreferences): Set<String> {
        return preferences.blockedTags
            .map(::normalizeTag)
            .filter { it.isNotBlank() }
            .toSet()
    }

    private val Rule34Post.normalizedTags: Set<String>
        get() = tags.map(::normalizeTag).filter { it.isNotBlank() }.toSet()

    private fun parseDelimitedTags(value: String?): List<String> {
        return value
            .orEmpty()
            .split(',')
            .map(::normalizeTag)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun extractEmbedSrc(raw: String): String? {
        if (raw.isBlank()) return null
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return raw
        }
        val match = Regex("src=['\\\"]([^'\\\"]+)['\\\"]").find(raw)?.groupValues?.getOrNull(1)
        return match?.let {
            when {
                it.startsWith("http://") || it.startsWith("https://") -> it
                it.startsWith("//") -> "https:$it"
                else -> null
            }
        }
    }

    private fun JsonElement?.objectArrayTags(fieldName: String): List<String> {
        return orEmptyArray()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { it[fieldName].primitiveContentOrNull() }
            .map(::normalizeTag)
            .filter { it.isNotBlank() }
    }

    private fun normalizeTag(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("&", " and ")
            .replace(Regex("[^a-z0-9()]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
    }

    private fun JsonElement?.primitiveContentOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement?.primitiveIntOrZero(): Int {
        return (this as? JsonPrimitive)?.intOrNull ?: 0
    }

    private fun JsonElement?.orEmptyArray(): JsonArray {
        return this as? JsonArray ?: JsonArray(emptyList())
    }

    private fun JsonElement?.firstObjectOrNull(): JsonObject? {
        return (this as? JsonArray)
            ?.firstOrNull()
            ?.let { it as? JsonObject }
    }
}

class BooruPagingSource(
    private val apiSource: BooruApiSource,
    private val settings: AppSettings,
    private val query: String,
    private val pageSize: Int,
) : PagingSource<Int, Rule34Post>() {
    override fun getRefreshKey(state: PagingState<Int, Rule34Post>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Rule34Post> {
        val page = params.key ?: 0
        return try {
            val posts = apiSource.searchPosts(
                settings = settings,
                rawQuery = query,
                page = page,
                limit = pageSize,
            )
            val hasNextPage = when (settings.selectedService) {
                BooruService.PORNHUB,
                BooruService.REDTUBE,
                BooruService.EPORNER,
                -> posts.isNotEmpty()

                else -> posts.size >= pageSize
            }
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (hasNextPage) page + 1 else null,
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}
