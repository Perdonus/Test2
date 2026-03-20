package com.perdonus.r34viewer.data.model

import com.perdonus.r34viewer.data.settings.ContentPreferences

object SearchQueryBuilder {
    private val whitespaceRegex = "\\s+".toRegex()

    fun build(
        service: BooruService,
        query: String,
        hideAiContent: Boolean,
        preferences: ContentPreferences,
    ): String {
        return if (service.usesTagSearch) {
            buildTagQuery(
                query = query,
                hideAiContent = hideAiContent,
                preferences = preferences,
            )
        } else {
            buildTextQuery(
                query = query,
                preferences = preferences,
            )
        }
    }

    private fun buildTagQuery(
        query: String,
        hideAiContent: Boolean,
        preferences: ContentPreferences,
    ): String {
        val preferredTags = linkedSetOf<String>()
        val blockedTags = linkedSetOf<String>()

        query
            .trim()
            .split(whitespaceRegex)
            .filter { it.isNotBlank() }
            .forEach { token ->
                val normalized = token.trim()
                when {
                    normalized.startsWith("-") && normalized.trimStart('-').isNotBlank() -> {
                        blockedTags += normalized.trimStart('-')
                    }

                    normalized.startsWith("+") && normalized.trimStart('+').isNotBlank() -> {
                        preferredTags += normalized.trimStart('+')
                    }

                    else -> {
                        preferredTags += normalized
                    }
                }
            }

        preferences.preferredTags.forEach { tag ->
            if (tag.isNotBlank() && tag !in blockedTags) {
                preferredTags += tag
            }
        }
        preferences.blockedTags.forEach { tag ->
            if (tag.isNotBlank()) {
                preferredTags.remove(tag)
                blockedTags += tag
            }
        }

        if (hideAiContent) {
            blockedTags += "ai_generated"
            blockedTags += "ai_assisted"
        }

        return buildList {
            addAll(preferredTags)
            addAll(blockedTags.map { "-$it" })
        }.joinToString(" ")
    }

    private fun buildTextQuery(
        query: String,
        preferences: ContentPreferences,
    ): String {
        val blockedTerms = preferences.blockedTags.toSet()
        val positiveTerms = linkedSetOf<String>()

        query
            .trim()
            .split(whitespaceRegex)
            .filter { it.isNotBlank() }
            .forEach { token ->
                val normalized = token.trim().trimStart('+')
                if (
                    normalized.isBlank() ||
                    token.startsWith("-") ||
                    normalized in blockedTerms
                ) {
                    return@forEach
                }
                positiveTerms += normalized
            }

        preferences.preferredTags.forEach { tag ->
            if (tag.isNotBlank() && tag !in blockedTerms) {
                positiveTerms += tag
            }
        }

        return positiveTerms
            .map { it.replace('_', ' ') }
            .joinToString(" ")
    }
}
