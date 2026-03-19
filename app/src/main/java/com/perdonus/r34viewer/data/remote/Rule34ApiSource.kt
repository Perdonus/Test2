package com.perdonus.r34viewer.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.perdonus.r34viewer.data.model.PostMediaType
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.model.SearchQueryBuilder
import com.perdonus.r34viewer.data.settings.AppSettings
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

class Rule34ApiException(message: String) : Exception(message)

class Rule34ApiSource(
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
        if (!settings.hasApiCredentials) {
            throw Rule34ApiException("Нужно заполнить user_id и api_key в настройках.")
        }

        val query = SearchQueryBuilder.build(rawQuery, settings.hideAiContent)
        val client = networkClientFactory.create(settings)
        val url = "https://api.rule34.xxx/index.php".toHttpUrl().newBuilder()
            .addQueryParameter("page", "dapi")
            .addQueryParameter("s", "post")
            .addQueryParameter("q", "index")
            .addQueryParameter("json", "1")
            .addQueryParameter("user_id", settings.apiUserId)
            .addQueryParameter("api_key", settings.apiKey)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("pid", page.toString())
            .addQueryParameter("tags", query)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Rule34ApiException("Ошибка API: HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty().trim()
            if (body.isEmpty()) return@withContext emptyList()

            val element = json.parseToJsonElement(body)
            return@withContext when (element) {
                is JsonArray -> element.mapNotNull { it.asPostOrNull(settings.hideAiContent) }
                is JsonObject -> throw Rule34ApiException(
                    element["message"]?.primitiveContentOrNull()
                        ?: "API вернул неожиданный объект.",
                )
                is JsonPrimitive -> throw Rule34ApiException(
                    element.primitiveContentOrNull() ?: "API вернул неожиданный ответ.",
                )
                else -> emptyList()
            }
        }
    }

    private fun JsonElement.asPostOrNull(hideAiContent: Boolean): Rule34Post? {
        val content = this as? JsonObject ?: return null
        val fileUrl = content["file_url"].primitiveContentOrNull().orEmpty()
        if (fileUrl.isBlank()) return null

        val tags = content["tags"]
            .primitiveContentOrNull()
            .orEmpty()
            .split(" ")
            .filter { it.isNotBlank() }

        val post = Rule34Post(
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

class Rule34PagingSource(
    private val apiSource: Rule34ApiSource,
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
