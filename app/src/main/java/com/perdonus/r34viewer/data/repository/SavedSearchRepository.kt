package com.perdonus.r34viewer.data.repository

import com.perdonus.r34viewer.data.local.SavedSearchDao
import com.perdonus.r34viewer.data.local.SavedSearchEntity
import com.perdonus.r34viewer.data.model.BooruService
import kotlinx.coroutines.flow.Flow

class SavedSearchRepository(
    private val dao: SavedSearchDao,
) {
    val savedSearches: Flow<List<SavedSearchEntity>> = dao.observeAll()

    fun savedSearches(service: BooruService): Flow<List<SavedSearchEntity>> = dao.observeAll(service.id)

    suspend fun save(query: String, service: BooruService): Boolean {
        if (query.isBlank()) return false
        val insertedId = dao.insert(
            SavedSearchEntity(
                serviceId = service.id,
                query = query,
                label = query,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return insertedId != -1L
    }

    suspend fun rename(id: Long, label: String) {
        dao.rename(id, label.ifBlank { "Без названия" })
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
    }
}
