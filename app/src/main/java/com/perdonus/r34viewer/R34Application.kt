package com.perdonus.r34viewer

import android.app.Application
import androidx.room.Room
import com.perdonus.r34viewer.data.local.AppDatabase
import com.perdonus.r34viewer.data.remote.AiTagResolver
import com.perdonus.r34viewer.data.remote.BooruApiSource
import com.perdonus.r34viewer.data.remote.NetworkClientFactory
import com.perdonus.r34viewer.data.repository.FavoritesRepository
import com.perdonus.r34viewer.data.repository.PostsRepository
import com.perdonus.r34viewer.data.repository.SavedSearchRepository
import com.perdonus.r34viewer.data.settings.SettingsRepository
import com.perdonus.r34viewer.data.settings.SettingsRepositoryImpl

class R34Application : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "r34_native.db",
    )
        .fallbackToDestructiveMigration()
        .build()

    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(application)
    val networkClientFactory = NetworkClientFactory()
    private val apiSource = BooruApiSource(networkClientFactory)
    val aiTagResolver = AiTagResolver(networkClientFactory)
    val postsRepository = PostsRepository(apiSource)
    val favoritesRepository = FavoritesRepository(database.favoritePostDao())
    val savedSearchRepository = SavedSearchRepository(database.savedSearchDao())
}
