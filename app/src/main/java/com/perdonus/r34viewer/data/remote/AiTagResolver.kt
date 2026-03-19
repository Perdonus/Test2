package com.perdonus.r34viewer.data.remote

import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.settings.AppSettings

class AiTagResolverException(message: String) : Exception(message)

data class AiResolvedQuery(
    val resolvedQuery: String,
    val explanation: String?,
)

class AiTagResolver(
    private val ruleServerStore: RuleServerStore,
) {
    suspend fun resolve(
        settings: AppSettings,
        service: BooruService,
        rawQuery: String,
        mode: ResolveMode = ResolveMode.AI,
    ): AiResolvedQuery {
        val normalizedQuery = rawQuery.trim()
        if (normalizedQuery.isBlank()) {
            throw AiTagResolverException("Введите запрос перед поиском.")
        }
        return try {
            ruleServerStore.resolveQuery(
                service = service,
                rawQuery = normalizedQuery,
                mode = mode,
            )
        } catch (exception: RuleServerException) {
            throw AiTagResolverException(exception.message ?: "Сервер поиска недоступен.")
        } catch (exception: Exception) {
            throw AiTagResolverException(exception.message ?: "Не удалось обработать запрос.")
        }
    }
}
