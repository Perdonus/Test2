package com.perdonus.r34viewer.data.model

import com.perdonus.r34viewer.data.settings.ContentPreferences

object SearchQueryBuilder {
    private val whitespaceRegex = "\\s+".toRegex()

    fun build(
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
                if (token.startsWith("-") && token.length > 1) {
                    blockedTags += token.removePrefix("-")
                } else {
                    preferredTags += token
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
}
