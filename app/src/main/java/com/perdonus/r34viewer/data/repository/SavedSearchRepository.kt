package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.local.SavedSearchEntity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.remote.RuleServerStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SavedSearchRepository(
    private val ruleServerStore: RuleServerStore,
) {
    val savedSearches: Flow<List<SavedSearchEntity>> = ruleServerStore.state.map { it.savedSearches }

    fun savedSearches(service: BooruService): Flow<List<SavedSearchEntity>> = ruleServerStore.state.map { snapshot ->
        snapshot.savedSearches.filter { it.serviceId == service.id }
    }

    suspend fun save(query: String, service: BooruService): Boolean {
        if (query.isBlank()) return false
        return ruleServerStore.saveSearch(query, service)
    }

    suspend fun rename(id: Long, label: String) {
        ruleServerStore.renameSearch(id, label.ifBlank { "Без названия" })
    }

    suspend fun delete(id: Long) {
        ruleServerStore.deleteSearch(id)
    }
}
