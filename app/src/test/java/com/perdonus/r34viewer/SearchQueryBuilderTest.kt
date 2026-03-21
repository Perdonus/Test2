package com.perdonus.r34viewer

import com.perdonus.r34viewer.data.model.SearchQueryBuilder
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.settings.ContentPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryBuilderTest {
    @Test
    fun `adds ai exclusions when filter enabled`() {
        val result = SearchQueryBuilder.build(
            service = BooruService.RULE34,
            query = "cat_ears 2girls",
            hideAiContent = true,
            preferences = ContentPreferences(),
        )

        assertEquals("cat_ears 2girls -ai_generated -ai_assisted", result)
    }

    @Test
    fun `keeps query unchanged when filter disabled`() {
        val result = SearchQueryBuilder.build(
            service = BooruService.RULE34,
            query = "video sound",
            hideAiContent = false,
            preferences = ContentPreferences(),
        )

        assertEquals("video sound", result)
    }

    @Test
    fun `text services keep only positive query terms without preference tags`() {
        val result = SearchQueryBuilder.build(
            service = BooruService.PORNHUB,
            query = "rukia -ai_generated",
            hideAiContent = true,
            preferences = ContentPreferences(
                preferredTags = listOf("big_ass"),
                blockedTags = listOf("loli"),
            ),
        )

        assertEquals("rukia", result)
    }

    @Test
    fun `tag services normalize explicit plus and minus modifiers`() {
        val result = SearchQueryBuilder.build(
            service = BooruService.RULE34,
            query = "++rukia --ai_generated +bleach",
            hideAiContent = false,
            preferences = ContentPreferences(),
        )

        assertEquals("rukia bleach -ai_generated", result)
    }
}
