package com.perdonus.r34viewer.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.remote.BooruApiSource
import com.perdonus.r34viewer.data.remote.BooruPagingSource
import com.perdonus.r34viewer.data.settings.AppSettings
import kotlinx.coroutines.flow.Flow

class PostsRepository(
    private val apiSource: BooruApiSource,
) {
    fun search(query: String, settings: AppSettings): Flow<PagingData<Rule34Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 8,
            ),
            pagingSourceFactory = {
                BooruPagingSource(
                    apiSource = apiSource,
                    settings = settings,
                    query = query,
                    pageSize = PAGE_SIZE,
                )
            },
        ).flow
    }

    private companion object {
        const val PAGE_SIZE = 40
    }
}
