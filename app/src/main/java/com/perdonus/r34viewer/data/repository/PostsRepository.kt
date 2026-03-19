package com.perdonus.r34viewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.remote.Rule34ApiSource
import com.perdonus.r34viewer.data.remote.Rule34PagingSource
import com.perdonus.r34viewer.data.settings.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostsRepository(
    private val apiSource: Rule34ApiSource,
) {
    fun search(query: String, settings: AppSettings): Flow<PagingData<Rule34Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                Rule34PagingSource(
                    apiSource = apiSource,
                    settings = settings,
                    query = query,
                    pageSize = PAGE_SIZE,
                )
            },
        ).flow.map { pagingData -> pagingData.map { it } }
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
