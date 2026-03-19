package com.perdonus.r34viewer.data.remote

import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap

class AiTagResolverException(message: String) : Exception(message)

data class AiResolvedQuery(
    val resolvedQuery: String,
    val explanation: String?,
)

class AiTagResolver(
    private val networkClientFactory: NetworkClientFactory,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val cache = ConcurrentHashMap<String, AiResolvedQuery>()

    suspend fun resolve(
        settings: AppSettings,
        service: BooruService,
        rawQuery: String,
    ): AiResolvedQuery = withContext(Dispatchers.IO) {
        val normalizedQuery = rawQuery.trim()
        if (normalizedQuery.isBlank()) {
            throw AiTagResolverException("Введите запрос перед AI-поиском.")
        }

        val cacheKey = "${service.id}|${normalizedQuery.lowercase()}"
        cache[cacheKey]?.let { return@withContext it }

        val client = networkClientFactory.create(settings)
        val body = buildJsonObject {
            put("model", ServiceSecrets.AI_MODEL)
            put("temperature", 0.1)
            put(
                "messages",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("role", "system")
                            put(
                                "content",
                                "You convert noisy human queries into booru tags. Return JSON only with exactly two top-level keys: query and explanation. Query must be a single string with space separated booru tags. Explanation must be a short Russian sentence. Prefer canonical booru tags, transliterate Cyrillic names, fix typos, for anime character names prefer surname_name style when known, keep tags lowercase with underscores, do not add rating tags, do not add AI filters, do not add markdown.",
                            )
                        },
                    )
                    add(
                        buildJsonObject {
                            put("role", "user")
                            put("content", "Service: ${service.displayName}. Raw query: $normalizedQuery")
                        },
                    )
                },
            )
        }.toString()

        val request = Request.Builder()
            .url("${ServiceSecrets.AI_BASE_URL}/chat/completions")
            .header("Authorization", "Bearer ${ServiceSecrets.AI_API_KEY}")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw AiTagResolverException("AI API вернул HTTP ${response.code}.")
            }

            val responseBody = response.body?.string().orEmpty()
            val content = extractAssistantContent(responseBody)
                ?: throw AiTagResolverException("AI API не вернул текст ответа.")
            val resolved = parseResolution(content)
            if (resolved.resolvedQuery.isBlank()) {
                throw AiTagResolverException("AI не смог подобрать теги.")
            }
            cache[cacheKey] = resolved
            return@withContext resolved
        }
    }

    private fun extractAssistantContent(body: String): String? {
        val root = json.parseToJsonElement(body) as? JsonObject ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val firstChoice = choices.firstOrNull() as? JsonObject ?: return null
        val message = firstChoice["message"] as? JsonObject ?: return null
        return (message["content"] as? JsonPrimitive)?.contentOrNull
    }

    private fun parseResolution(content: String): AiResolvedQuery {
        val cleaned = content
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val root = json.parseToJsonElement(cleaned) as? JsonObject
            ?: throw AiTagResolverException("AI вернул невалидный JSON.")

        return AiResolvedQuery(
            resolvedQuery = (root["query"] as? JsonPrimitive)?.contentOrNull().orEmpty().trim(),
            explanation = (root["explanation"] as? JsonPrimitive)?.contentOrNull()?.trim(),
        )
    }
}
