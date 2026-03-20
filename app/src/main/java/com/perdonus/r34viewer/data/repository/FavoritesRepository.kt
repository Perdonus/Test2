package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.remote.RuleServerStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val ruleServerStore: RuleServerStore,
) {
    val favorites: Flow<List<Rule34Post>> = ruleServerStore.state.map { it.favorites }

    fun favorites(service: BooruService): Flow<List<Rule34Post>> = ruleServerStore.state.map { snapshot ->
        snapshot.favorites.filter { it.service == service }
    }

    val favoriteIds: Flow<Set<String>> = ruleServerStore.state.map { it.favoriteIds }

    suspend fun toggle(post: Rule34Post) {
        ruleServerStore.toggleFavorite(post)
    }

    suspend fun remove(service: BooruService, postId: String) {
        val existing = ruleServerStore.state.value.favorites.firstOrNull {
            it.service == service && it.id == postId
        } ?: return
        ruleServerStore.toggleFavorite(existing)
    }
}
