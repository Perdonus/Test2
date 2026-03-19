package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.local.FavoritePostDao
import com.perdonus.r34viewer.data.local.toDomain
import com.perdonus.r34viewer.data.local.toEntity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoritesRepository(
    private val dao: FavoritePostDao,
) {
    val favorites: Flow<List<Rule34Post>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    fun favorites(service: BooruService): Flow<List<Rule34Post>> = dao.observeAll(service.id).map { list ->
        list.map { it.toDomain() }
    }

    val favoriteIds: Flow<Set<String>> = dao.observeAll().map { list ->
        list.map { "${it.serviceId}:${it.id}" }.toSet()
    }

    suspend fun toggle(post: Rule34Post) {
        val existing = dao.getById(post.service.id, post.id)
        if (existing == null) {
            dao.upsert(post.toEntity())
        } else {
            dao.deleteById(post.service.id, post.id)
        }
    }

    suspend fun remove(service: BooruService, postId: Int) {
        dao.deleteById(service.id, postId)
    }
}
