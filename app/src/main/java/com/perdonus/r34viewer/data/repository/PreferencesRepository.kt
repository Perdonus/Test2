package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.remote.RuleServerStore
import com.perdonus.r34viewer.data.settings.ContentPreferences
import com.perdonus.r34viewer.data.settings.PreferenceCatalogItem

class PreferencesRepository(
    private val ruleServerStore: RuleServerStore,
) {
    suspend fun refresh() {
        ruleServerStore.refresh()
    }

    suspend fun save(preferences: ContentPreferences): ContentPreferences {
        return ruleServerStore.updatePreferences(preferences)
    }

    suspend fun searchCatalog(
        service: BooruService,
        query: String,
    ): List<PreferenceCatalogItem> {
        return ruleServerStore.searchPreferenceCatalog(service, query)
    }
}
