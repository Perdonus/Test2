package com.perdonus.r34viewer.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.PostMediaType
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.model.SearchQueryBuilder
import com.perdonus.r34viewer.data.settings.AppSettings
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
            query = rawQuery,
            hideAiContent = settings.hideAiContent,
            preferences = settings.preferences,
        )
        validateServiceApiConfig(service, settings.serviceApiConfig)
        val client = networkClientFactory.create(settings)
        val request = Request.Builder()
            .url(buildUrl(service, query, page, limit, settings.serviceApiConfig))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw BooruApiException("${service.displayName}: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty().trim()
            if (body.isEmpty()) return@withContext emptyList()

            val element = json.parseToJsonElement(body)
            return@withContext when (element) {
                is JsonArray -> element.mapNotNull { it.asPostOrNull(service, settings.hideAiContent) }
                is JsonObject -> throw BooruApiException(
                    element["message"]?.primitiveContentOrNull()
                        ?: "${service.displayName} вернул неожиданный объект.",
                )
                is JsonPrimitive -> throw BooruApiException(
                    element.primitiveContentOrNull() ?: "${service.displayName} вернул неожиданный ответ.",
                )
                else -> emptyList()
            }
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

    private fun JsonElement.asPostOrNull(
        service: BooruService,
        hideAiContent: Boolean,
    ): Rule34Post? {
        val content = this as? JsonObject ?: return null
        val fileUrl = content["file_url"].primitiveContentOrNull().orEmpty()
        if (fileUrl.isBlank()) return null

        val tags = content["tags"]
            .primitiveContentOrNull()
            .orEmpty()
            .split(" ")
            .filter { it.isNotBlank() }

        val post = Rule34Post(
            service = service,
            id = content["id"].primitiveIntOrZero(),
            previewUrl = content["preview_url"].primitiveContentOrNull(),
            sampleUrl = content["sample_url"].primitiveContentOrNull(),
            fileUrl = fileUrl,
            tags = tags,
            rating = content["rating"].primitiveContentOrNull().orEmpty(),
            score = content["score"].primitiveIntOrZero(),
            width = content["width"].primitiveIntOrZero(),
            height = content["height"].primitiveIntOrZero(),
            mediaType = PostMediaType.fromUrl(fileUrl),
        )

        return if (hideAiContent && post.areTagsAiRelated) null else post
    }

    private fun JsonElement?.primitiveContentOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement?.primitiveIntOrZero(): Int {
        return (this as? JsonPrimitive)?.intOrNull ?: 0
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
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (posts.size < pageSize) null else page + 1,
            )
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}
