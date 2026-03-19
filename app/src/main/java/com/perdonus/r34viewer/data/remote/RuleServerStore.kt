package com.perdonus.r34viewer.data.remote

import com.perdonus.r34viewer.data.local.SavedSearchEntity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.PostMediaType
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.settings.AiApiConfig
import com.perdonus.r34viewer.data.settings.KonachanApiConfig
import com.perdonus.r34viewer.data.settings.ProxyConfig
import com.perdonus.r34viewer.data.settings.ProxyType
import com.perdonus.r34viewer.data.settings.Rule34ApiConfig
import com.perdonus.r34viewer.data.settings.ServiceApiConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Proxy

private const val RULE_SERVER_BASE_URL = "https://sosiskibot.ru/rule/api"

enum class ResolveMode(
    val wireName: String,
) {
    AUTO("auto"),
    AI("ai"),
}

data class RuleServerSnapshot(
    val isLoaded: Boolean = false,
    val favorites: List<Rule34Post> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val savedSearches: List<SavedSearchEntity> = emptyList(),
    val proxyConfig: ProxyConfig = ProxyConfig(),
    val serviceApiConfig: ServiceApiConfig = ServiceApiConfig(),
)

class RuleServerException(message: String) : Exception(message)

class RuleServerStore {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(RuleServerSnapshot())
    val state = _state.asStateFlow()

    init {
        scope.launch {
            while (currentCoroutineContext().isActive) {
                runCatching { refresh() }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val root = getJson("/state")
        _state.value = RuleServerSnapshot(
            isLoaded = true,
            favorites = root["favorites"].jsonArrayOrEmpty().mapNotNull { it.asPostOrNull() },
            favoriteIds = root["favoriteIds"].jsonArrayOrEmpty().mapNotNull { it.asStringOrNull() }.toSet(),
            savedSearches = root["savedSearches"].jsonArrayOrEmpty().mapNotNull { it.asSavedSearchOrNull() },
            proxyConfig = root["proxy"].asProxyConfig(),
            serviceApiConfig = root["apiConfig"].asServiceApiConfig(),
        )
    }

    suspend fun toggleFavorite(post: Rule34Post): Boolean = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/favorites/toggle",
            body = buildJsonObject {
                put("serviceId", JsonPrimitive(post.service.id))
                put("id", JsonPrimitive(post.id))
                put("previewUrl", post.previewUrl?.let(::JsonPrimitive) ?: JsonNull)
                put("sampleUrl", post.sampleUrl?.let(::JsonPrimitive) ?: JsonNull)
                put("fileUrl", JsonPrimitive(post.fileUrl))
                put("tags", JsonArray(post.tags.map(::JsonPrimitive)))
                put("rating", JsonPrimitive(post.rating))
                put("score", JsonPrimitive(post.score))
                put("width", JsonPrimitive(post.width))
                put("height", JsonPrimitive(post.height))
                put("mediaType", JsonPrimitive(post.mediaType.name))
            },
        )
        val favorites = root["favorites"].jsonArrayOrEmpty().mapNotNull { it.asPostOrNull() }
        val favoriteIds = root["favoriteIds"].jsonArrayOrEmpty().mapNotNull { it.asStringOrNull() }.toSet()
        _state.value = _state.value.copy(
            isLoaded = true,
            favorites = favorites,
            favoriteIds = favoriteIds,
        )
        (root["changed"] as? JsonPrimitive)?.content == "true"
    }

    suspend fun saveSearch(
        query: String,
        service: BooruService,
        label: String = query,
    ): Boolean = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/saved-searches",
            body = buildJsonObject {
                put("serviceId", JsonPrimitive(service.id))
                put("query", JsonPrimitive(query))
                put("label", JsonPrimitive(label))
            },
        )
        val savedSearches = root["savedSearches"].jsonArrayOrEmpty().mapNotNull { it.asSavedSearchOrNull() }
        _state.value = _state.value.copy(
            isLoaded = true,
            savedSearches = savedSearches,
        )
        (root["saved"] as? JsonPrimitive)?.content == "true"
    }

    suspend fun renameSearch(
        id: Long,
        label: String,
    ) = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/saved-searches/rename",
            body = buildJsonObject {
                put("id", JsonPrimitive(id))
                put("label", JsonPrimitive(label))
            },
        )
        _state.value = _state.value.copy(
            isLoaded = true,
            savedSearches = root["savedSearches"].jsonArrayOrEmpty().mapNotNull { it.asSavedSearchOrNull() },
        )
    }

    suspend fun deleteSearch(id: Long) = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/saved-searches/delete",
            body = buildJsonObject {
                put("id", JsonPrimitive(id))
            },
        )
        _state.value = _state.value.copy(
            isLoaded = true,
            savedSearches = root["savedSearches"].jsonArrayOrEmpty().mapNotNull { it.asSavedSearchOrNull() },
        )
    }

    suspend fun updateProxy(proxyConfig: ProxyConfig): ProxyConfig = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/proxy",
            body = buildJsonObject {
                put("enabled", JsonPrimitive(proxyConfig.enabled))
                put("type", JsonPrimitive(proxyConfig.type.name))
                put("host", JsonPrimitive(proxyConfig.host))
                proxyConfig.port?.let { put("port", JsonPrimitive(it)) }
                put("username", JsonPrimitive(proxyConfig.username))
                put("password", JsonPrimitive(proxyConfig.password))
            },
        )
        val proxy = root["proxy"].asProxyConfig()
        _state.value = _state.value.copy(
            isLoaded = true,
            proxyConfig = proxy,
        )
        proxy
    }

    suspend fun updateServerSettings(
        proxyConfig: ProxyConfig,
        serviceApiConfig: ServiceApiConfig,
    ) = withContext(Dispatchers.IO) {
        updateProxy(proxyConfig)
        updateServiceApiConfig(serviceApiConfig)
    }

    suspend fun updateServiceApiConfig(serviceApiConfig: ServiceApiConfig): ServiceApiConfig = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/api-config",
            body = buildJsonObject {
                putJsonObject("rule34") {
                    put("userId", JsonPrimitive(serviceApiConfig.rule34.userId))
                    put("apiKey", JsonPrimitive(serviceApiConfig.rule34.apiKey))
                }
                putJsonObject("konachan") {
                    put("apiKey", JsonPrimitive(serviceApiConfig.konachan.apiKey))
                    put("username", JsonPrimitive(serviceApiConfig.konachan.username))
                    put("password", JsonPrimitive(serviceApiConfig.konachan.password))
                    put("email", JsonPrimitive(serviceApiConfig.konachan.email))
                }
                putJsonObject("ai") {
                    put("baseUrl", JsonPrimitive(serviceApiConfig.ai.baseUrl))
                    put("apiKey", JsonPrimitive(serviceApiConfig.ai.apiKey))
                    put("model", JsonPrimitive(serviceApiConfig.ai.model))
                }
            },
        )
        val serviceConfig = root["apiConfig"].asServiceApiConfig()
        _state.value = _state.value.copy(
            isLoaded = true,
            serviceApiConfig = serviceConfig,
        )
        serviceConfig
    }

    suspend fun resolveQuery(
        service: BooruService,
        rawQuery: String,
        mode: ResolveMode,
    ): AiResolvedQuery = withContext(Dispatchers.IO) {
        val root = postJson(
            path = "/resolve-query",
            body = buildJsonObject {
                put("serviceId", JsonPrimitive(service.id))
                put("query", JsonPrimitive(rawQuery))
                put("mode", JsonPrimitive(mode.wireName))
            },
        )
        val query = root["resolvedQuery"].asStringOrNull().orEmpty().trim()
        if (query.isBlank()) {
            throw RuleServerException("Сервер не смог подобрать запрос.")
        }
        return@withContext AiResolvedQuery(
            resolvedQuery = query,
            explanation = root["explanation"].asStringOrNull()?.trim(),
        )
    }

    private suspend fun getJson(path: String): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$RULE_SERVER_BASE_URL$path")
            .header("User-Agent", "R34NativeAndroid/1.1.0")
            .get()
            .build()
        executeJson(request)
    }

    private suspend fun postJson(
        path: String,
        body: JsonObject,
    ): JsonObject = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$RULE_SERVER_BASE_URL$path")
            .header("User-Agent", "R34NativeAndroid/1.1.0")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        executeJson(request)
    }

    private fun executeJson(request: Request): JsonObject {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    (json.parseToJsonElement(body) as JsonObject)["error"].asStringOrNull()
                }.getOrNull()
                throw RuleServerException(message ?: "Сервер rule вернул HTTP ${response.code}.")
            }
            val root = json.parseToJsonElement(body) as? JsonObject
                ?: throw RuleServerException("Сервер rule вернул невалидный JSON.")
            return root
        }
    }

    private fun JsonElement?.asStringOrNull(): String? {
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement?.jsonArrayOrEmpty(): JsonArray {
        return this as? JsonArray ?: JsonArray(emptyList())
    }

    private fun JsonElement?.asProxyConfig(): ProxyConfig {
        val root = this as? JsonObject ?: return ProxyConfig()
        val type = runCatching {
            ProxyType.valueOf(root["type"].asStringOrNull() ?: ProxyType.HTTP.name)
        }.getOrDefault(ProxyType.HTTP)
        return ProxyConfig(
            enabled = root["enabled"].asStringOrNull() == "true",
            type = type,
            host = root["host"].asStringOrNull().orEmpty(),
            port = (root["port"] as? JsonPrimitive)?.content?.toIntOrNull(),
            username = root["username"].asStringOrNull().orEmpty(),
            password = root["password"].asStringOrNull().orEmpty(),
        )
    }

    private fun JsonElement?.asServiceApiConfig(): ServiceApiConfig {
        val root = this as? JsonObject ?: return ServiceApiConfig()
        val rule34 = root["rule34"] as? JsonObject
        val konachan = root["konachan"] as? JsonObject
        val ai = root["ai"] as? JsonObject
        return ServiceApiConfig(
            rule34 = Rule34ApiConfig(
                userId = rule34?.get("userId").asStringOrNull().orEmpty(),
                apiKey = rule34?.get("apiKey").asStringOrNull().orEmpty(),
            ),
            konachan = KonachanApiConfig(
                apiKey = konachan?.get("apiKey").asStringOrNull().orEmpty(),
                username = konachan?.get("username").asStringOrNull().orEmpty(),
                password = konachan?.get("password").asStringOrNull().orEmpty(),
                email = konachan?.get("email").asStringOrNull().orEmpty(),
            ),
            ai = AiApiConfig(
                baseUrl = ai?.get("baseUrl").asStringOrNull().orEmpty(),
                apiKey = ai?.get("apiKey").asStringOrNull().orEmpty(),
                model = ai?.get("model").asStringOrNull().orEmpty(),
            ),
        )
    }

    private fun JsonElement?.asSavedSearchOrNull(): SavedSearchEntity? {
        val root = this as? JsonObject ?: return null
        return SavedSearchEntity(
            id = (root["id"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
            serviceId = root["serviceId"].asStringOrNull().orEmpty(),
            query = root["query"].asStringOrNull().orEmpty(),
            label = root["label"].asStringOrNull().orEmpty(),
            createdAt = (root["createdAt"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L,
        )
    }

    private fun JsonElement?.asPostOrNull(): Rule34Post? {
        val root = this as? JsonObject ?: return null
        val fileUrl = root["fileUrl"].asStringOrNull().orEmpty()
        if (fileUrl.isBlank()) return null
        return Rule34Post(
            service = BooruService.fromId(root["serviceId"].asStringOrNull()),
            id = (root["id"] as? JsonPrimitive)?.content?.toIntOrNull() ?: return null,
            previewUrl = root["previewUrl"].asStringOrNull(),
            sampleUrl = root["sampleUrl"].asStringOrNull(),
            fileUrl = fileUrl,
            tags = root["tags"].jsonArrayOrEmpty().mapNotNull { it.asStringOrNull() },
            rating = root["rating"].asStringOrNull().orEmpty(),
            score = (root["score"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
            width = (root["width"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
            height = (root["height"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
            mediaType = runCatching {
                PostMediaType.valueOf(root["mediaType"].asStringOrNull().orEmpty())
            }.getOrDefault(PostMediaType.fromUrl(fileUrl)),
        )
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 30_000L
    }
}
