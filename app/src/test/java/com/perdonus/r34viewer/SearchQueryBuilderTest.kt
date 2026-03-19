package com.perdonus.r34viewer

import com.perdonus.r34viewer.data.model.SearchQueryBuilder
import com.perdonus.r34viewer.data.settings.ContentPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryBuilderTest {
    @Test
    fun `adds ai exclusions when filter enabled`() {
        val result = SearchQueryBuilder.build(
            query = "cat_ears 2girls",
            hideAiContent = true,
            preferences = ContentPreferences(),
        )

        assertEquals("cat_ears 2girls -ai_generated -ai_assisted", result)
    }

    @Test
    fun `keeps query unchanged when filter disabled`() {
        val result = SearchQueryBuilder.build(
            query = "video sound",
            hideAiContent = false,
            preferences = ContentPreferences(),
        )

        assertEquals("video sound", result)
    }
}
