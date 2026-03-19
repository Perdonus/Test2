package com.perdonus.r34viewer

import android.app.Application
import com.perdonus.r34viewer.data.remote.AiTagResolver
import com.perdonus.r34viewer.data.remote.BooruApiSource
import com.perdonus.r34viewer.data.remote.NetworkClientFactory
import com.perdonus.r34viewer.data.remote.RuleServerStore
import com.perdonus.r34viewer.data.repository.FavoritesRepository
import com.perdonus.r34viewer.data.repository.PostsRepository
import com.perdonus.r34viewer.data.repository.PreferencesRepository
import com.perdonus.r34viewer.data.repository.SavedSearchRepository
import com.perdonus.r34viewer.data.settings.SettingsRepository
import com.perdonus.r34viewer.data.settings.SettingsRepositoryImpl

class R34Application : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val ruleServerStore = RuleServerStore()
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(application, ruleServerStore)
    val networkClientFactory = NetworkClientFactory()
    private val apiSource = BooruApiSource(networkClientFactory)
    val aiTagResolver = AiTagResolver(ruleServerStore)
    val postsRepository = PostsRepository(apiSource)
    val favoritesRepository = FavoritesRepository(ruleServerStore)
    val savedSearchRepository = SavedSearchRepository(ruleServerStore)
    val preferencesRepository = PreferencesRepository(ruleServerStore)
}
