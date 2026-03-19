package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.local.FavoritePostDao
import com.perdonus.r34viewer.data.local.toDomain
import com.perdonus.r34viewer.data.local.toEntity
import com.perdonus.r34viewer.data.model.Rule34Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val dao: FavoritePostDao,
) {
    val favorites: Flow<List<Rule34Post>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val favoriteIds: Flow<Set<Int>> = dao.observeIds().map { it.toSet() }

    suspend fun toggle(post: Rule34Post) {
        val existing = dao.getById(post.id)
        if (existing == null) {
            dao.upsert(post.toEntity())
        } else {
            dao.deleteById(post.id)
        }
    }

    suspend fun remove(postId: Int) {
        dao.deleteById(postId)
    }
}
