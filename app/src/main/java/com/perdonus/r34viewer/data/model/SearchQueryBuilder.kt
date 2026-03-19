package com.perdonus.r34viewer.data.model

object SearchQueryBuilder {
    private val whitespaceRegex = "\\s+".toRegex()

    fun build(query: String, hideAiContent: Boolean): String {
        val tokens = query
            .trim()
            .split(whitespaceRegex)
            .filter { it.isNotBlank() }
            .toMutableList()

        if (hideAiContent) {
            if ("-ai_generated" !in tokens) {
                tokens += "-ai_generated"
            }
            if ("-ai_assisted" !in tokens) {
                tokens += "-ai_assisted"
            }
        }

        return tokens.joinToString(" ")
    }
}
