package com.perdonus.r34viewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.perdonus.r34viewer.R34Application
import com.perdonus.r34viewer.data.cache.ImageMemoryCache
import com.perdonus.r34viewer.data.cache.VideoPlaybackCache
import com.perdonus.r34viewer.data.local.SavedSearchEntity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.remote.AiTagResolver
import com.perdonus.r34viewer.data.remote.AiTagResolverException
import com.perdonus.r34viewer.data.remote.ResolveMode
import com.perdonus.r34viewer.data.repository.FavoritesRepository
import com.perdonus.r34viewer.data.repository.PostsRepository
import com.perdonus.r34viewer.data.repository.PreferencesRepository
import com.perdonus.r34viewer.data.repository.SavedSearchRepository
import com.perdonus.r34viewer.data.settings.PreferenceCatalogItem
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.data.settings.AiApiConfig
import com.perdonus.r34viewer.data.settings.ContentPreferences
import com.perdonus.r34viewer.data.settings.KonachanApiConfig
import com.perdonus.r34viewer.data.cache.MediaDiskCache
import com.perdonus.r34viewer.data.settings.ProxyConfig
import com.perdonus.r34viewer.data.settings.ProxyType
import com.perdonus.r34viewer.data.settings.Rule34ApiConfig
import com.perdonus.r34viewer.data.settings.ServiceApiConfig
import com.perdonus.r34viewer.data.settings.SettingsRepository
import com.perdonus.r34viewer.data.settings.SettingsValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {
    private val _selectedPost = MutableStateFlow<Rule34Post?>(null)
    val selectedPost = _selectedPost.asStateFlow()

    fun selectPost(post: Rule34Post) {
        _selectedPost.value = post
    }
}

class SearchViewModel(
    private val postsRepository: PostsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val savedSearchRepository: SavedSearchRepository,
    private val preferencesRepository: PreferencesRepository,
    private val settingsRepository: SettingsRepository,
    private val aiTagResolver: AiTagResolver,
) : ViewModel() {
    data class SearchRequest(
        val query: String = "",
        val originQuery: String = "",
        val nonce: Long = 0,
    )

    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage = _feedbackMessage.asStateFlow()

    private val _searchSuggestions = MutableStateFlow(emptyList<PreferenceCatalogItem>())
    val searchSuggestions = _searchSuggestions.asStateFlow()

    private val _isResolvingQuery = MutableStateFlow(false)
    val isResolvingQuery = _isResolvingQuery.asStateFlow()

    private val _searchRequest = MutableStateFlow(SearchRequest())
    private var suggestionJob: Job? = null

    val hasSubmittedSearch = _searchRequest
        .map { it.nonce > 0L }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            false,
        )

    val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    val favoriteIds = favoritesRepository.favoriteIds.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet<String>(),
    )

    val pagingData = combine(_searchRequest, settings) { request, appSettings ->
        request to appSettings
    }.flatMapLatest { (request, appSettings) ->
        if (request.nonce == 0L) {
            flowOf(PagingData.empty<Rule34Post>())
        } else {
            postsRepository.search(request.query, appSettings)
        }
    }.cachedIn(viewModelScope)

    fun updateQuery(query: String) {
        _queryText.value = query
        requestSuggestions(query)
    }

    fun submitSearch() {
        val rawQuery = _queryText.value.trim()
        if (_isResolvingQuery.value) return
        clearSuggestions()

        if (rawQuery.isBlank()) {
            _queryText.value = ""
            _feedbackMessage.value = null
            _searchRequest.value = SearchRequest(
                query = "",
                originQuery = "",
                nonce = _searchRequest.value.nonce + 1,
            )
            return
        }

        runSearch(rawQuery)
    }

    fun runSearch(
        query: String,
        service: BooruService? = null,
    ) {
        viewModelScope.launch {
            if (service != null && settings.value.selectedService != service) {
                settingsRepository.updateSelectedService(service)
            }
            val normalized = query.trim()
            _queryText.value = normalized
            _feedbackMessage.value = null
            clearSuggestions()
            _searchRequest.value = SearchRequest(
                query = normalized,
                originQuery = normalized,
                nonce = _searchRequest.value.nonce + 1,
            )
        }
    }

    fun clearMessage() {
        _feedbackMessage.value = null
    }

    fun saveCurrentSearch() {
        val query = _queryText.value.trim()
        val service = settings.value.selectedService
        if (query.isBlank()) {
            _feedbackMessage.value = "Сначала введите запрос."
            return
        }
        viewModelScope.launch {
            runCatching {
                savedSearchRepository.save(query, service)
            }.onSuccess { saved ->
                _feedbackMessage.value = if (saved) {
                    "Запрос сохранён в ${service.displayName}."
                } else {
                    "Такой запрос уже есть в закладках ${service.displayName}."
                }
            }.onFailure { error ->
                _feedbackMessage.value = error.message ?: "Не удалось сохранить запрос на сервере."
            }
        }
    }

    fun toggleFavorite(post: Rule34Post) {
        viewModelScope.launch {
            runCatching {
                favoritesRepository.toggle(post)
            }.onFailure { error ->
                _feedbackMessage.value = error.message ?: "Не удалось обновить избранное."
            }
        }
    }

    fun updateHideAiContent(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAiFilter(enabled)
        }
    }

    fun selectService(service: BooruService) {
        viewModelScope.launch {
            if (settings.value.selectedService == service) return@launch
            settingsRepository.updateSelectedService(service)
            requestSuggestions(_queryText.value, service)
            val currentRequest = _searchRequest.value
            val activeQuery = _queryText.value.trim()
            if (currentRequest.nonce > 0L) {
                _queryText.value = activeQuery
                _feedbackMessage.value = null
                clearSuggestions()
                _searchRequest.value = SearchRequest(
                    query = activeQuery,
                    originQuery = activeQuery,
                    nonce = _searchRequest.value.nonce + 1,
                )
            }
        }
    }

    fun runAiSearch() {
        val rawQuery = _queryText.value.trim()
        if (rawQuery.isBlank() || _isResolvingQuery.value) return

        viewModelScope.launch {
            executeResolvedSearch(
                rawQuery = rawQuery,
                mode = ResolveMode.AI,
            )
        }
    }

    private suspend fun executeResolvedSearch(
        rawQuery: String,
        mode: ResolveMode,
        serviceOverride: BooruService? = null,
        fallbackMessage: String? = null,
    ) {
        _isResolvingQuery.value = true
        try {
            val currentSettings = settings.value
            val targetService = serviceOverride ?: currentSettings.selectedService
            val resolution = aiTagResolver.resolve(
                settings = currentSettings,
                service = targetService,
                rawQuery = rawQuery,
                mode = mode,
            )
            val finalQuery = resolution.resolvedQuery.ifBlank { rawQuery }
            _queryText.value = finalQuery
            _feedbackMessage.value = null
            clearSuggestions()
            _searchRequest.value = SearchRequest(
                query = finalQuery,
                originQuery = rawQuery,
                nonce = _searchRequest.value.nonce + 1,
            )
        } catch (exception: AiTagResolverException) {
            _queryText.value = rawQuery
            _feedbackMessage.value = if (mode == ResolveMode.AI) {
                exception.message ?: "ИИ-поиск не сработал."
            } else {
                fallbackMessage ?: "Не удалось уточнить запрос, ищу как есть."
            }
            clearSuggestions()
            _searchRequest.value = SearchRequest(
                query = rawQuery,
                originQuery = rawQuery,
                nonce = _searchRequest.value.nonce + 1,
            )
        } finally {
            _isResolvingQuery.value = false
        }
    }

    fun useSuggestion(tag: String) {
        val service = settings.value.selectedService
        val query = if (service.usesTagSearch) tag else tag.replace('_', ' ')
        runSearch(query, service)
    }

    private fun requestSuggestions(
        query: String,
        serviceOverride: BooruService? = null,
    ) {
        val normalized = query.trim()
        suggestionJob?.cancel()
        if (normalized.isBlank()) {
            clearSuggestions()
            return
        }

        suggestionJob = viewModelScope.launch {
            delay(180)
            val service = serviceOverride ?: settings.value.selectedService
            runCatching {
                preferencesRepository.searchCatalog(
                    service = service,
                    query = normalized,
                )
            }.onSuccess { items ->
                val serviceStillMatches = serviceOverride != null || settings.value.selectedService == service
                if (_queryText.value.trim() == normalized && serviceStillMatches) {
                    _searchSuggestions.value = items
                        .filter { it.tag != normalized }
                        .take(10)
                }
            }.onFailure {
                if (_queryText.value.trim() == normalized) {
                    _searchSuggestions.value = emptyList()
                }
            }
        }
    }

    private fun clearSuggestions() {
        suggestionJob?.cancel()
        _searchSuggestions.value = emptyList()
    }
}

class FavoritesViewModel(
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    val favorites = favoritesRepository.favorites.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun toggleFavorite(post: Rule34Post) {
        viewModelScope.launch {
            runCatching {
                favoritesRepository.toggle(post)
            }
        }
    }
}

class SavedSearchesViewModel(
    private val savedSearchRepository: SavedSearchRepository,
) : ViewModel() {
    val savedSearches = savedSearchRepository.savedSearches.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList<SavedSearchEntity>(),
    )

    fun delete(id: Long) {
        viewModelScope.launch {
            runCatching {
                savedSearchRepository.delete(id)
            }
        }
    }

    fun rename(id: Long, label: String) {
        viewModelScope.launch {
            runCatching {
                savedSearchRepository.rename(id, label)
            }
        }
    }
}

data class PreferencesUiState(
    val selectedService: BooruService = BooruService.RULE34,
    val preferredTags: List<String> = emptyList(),
    val blockedTags: List<String> = emptyList(),
    val catalogQuery: String = "",
    val catalogItems: List<PreferenceCatalogItem> = emptyList(),
    val titleByTag: Map<String, String> = emptyMap(),
    val isSearching: Boolean = false,
    val message: String? = null,
)

class PreferencesViewModel(
    private val settingsRepository: SettingsRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    private val settings = settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppSettings(),
    )

    private val _catalogQuery = MutableStateFlow("")
    private val _catalogItems = MutableStateFlow(emptyList<PreferenceCatalogItem>())
    private val _isSearching = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    val state = combine(
        settings,
        _catalogQuery,
        _catalogItems,
        _isSearching,
        _message,
    ) { appSettings, query, items, isSearching, message ->
        val titleByTag = buildMap {
            putAll(appSettings.preferenceTitles)
            items.forEach { put(it.tag, it.titleRu) }
        }
        PreferencesUiState(
            selectedService = appSettings.selectedService,
            preferredTags = appSettings.preferences.preferredTags,
            blockedTags = appSettings.preferences.blockedTags,
            catalogQuery = query,
            catalogItems = items,
            titleByTag = titleByTag,
            isSearching = isSearching,
            message = message,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PreferencesUiState(),
    )

    fun updateCatalogQuery(value: String) {
        _catalogQuery.value = value
        if (value.isBlank()) {
            _catalogItems.value = emptyList()
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refreshCatalog() {
        searchCatalogInternal(showEmptyMessage = false)
    }

    fun searchCatalog() {
        searchCatalogInternal(showEmptyMessage = true)
    }

    private fun searchCatalogInternal(showEmptyMessage: Boolean) {
        val query = _catalogQuery.value.trim()
        if (_isSearching.value) return

        viewModelScope.launch {
            val selectedService = settings.value.selectedService
            _isSearching.value = true
            _message.value = null
            runCatching {
                preferencesRepository.refresh()
                preferencesRepository.searchCatalog(
                    service = selectedService,
                    query = query,
                )
            }.onSuccess { items ->
                _catalogItems.value = items
                _message.value = if (items.isEmpty() && showEmptyMessage) {
                    "По тегам ничего не нашлось для ${selectedService.displayName}."
                } else {
                    null
                }
            }.onFailure { error ->
                _message.value = error.message ?: "Не удалось получить список тегов."
            }
            _isSearching.value = false
        }
    }

    fun addPreferred(tag: String) {
        val current = settings.value.preferences
        savePreferences(
            ContentPreferences(
                preferredTags = (current.preferredTags + tag).distinct(),
                blockedTags = current.blockedTags.filterNot { it == tag },
            ),
        )
    }

    fun addBlocked(tag: String) {
        val current = settings.value.preferences
        savePreferences(
            ContentPreferences(
                preferredTags = current.preferredTags.filterNot { it == tag },
                blockedTags = (current.blockedTags + tag).distinct(),
            ),
        )
    }

    fun removePreferred(tag: String) {
        val current = settings.value.preferences
        savePreferences(
            current.copy(
                preferredTags = current.preferredTags.filterNot { it == tag },
            ),
        )
    }

    fun removeBlocked(tag: String) {
        val current = settings.value.preferences
        savePreferences(
            current.copy(
                blockedTags = current.blockedTags.filterNot { it == tag },
            ),
        )
    }

    private fun savePreferences(preferences: ContentPreferences) {
        viewModelScope.launch {
            runCatching {
                preferencesRepository.save(preferences)
            }.onSuccess {
                _message.value = "Предпочтения сохранены на сервере."
            }.onFailure { error ->
                _message.value = error.message ?: "Не удалось сохранить предпочтения."
            }
        }
    }
}

data class SettingsFormState(
    val isLoaded: Boolean = false,
    val rule34UserId: String = "",
    val rule34ApiKey: String = "",
    val konachanApiKey: String = "",
    val konachanUsername: String = "",
    val konachanPassword: String = "",
    val konachanEmail: String = "",
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "",
    val proxyEnabled: Boolean = false,
    val proxyType: ProxyType = ProxyType.HTTP,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val cacheSizeBytes: Long = 0L,
    val cacheLimitBytes: Long = -1L,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsFormState())
    val state = _state.asStateFlow()
    private var hasPendingChanges = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (_state.value.isLoaded && hasPendingChanges) return@collect
                _state.value = SettingsFormState(
                    isLoaded = true,
                    rule34UserId = settings.serviceApiConfig.rule34.userId,
                    rule34ApiKey = settings.serviceApiConfig.rule34.apiKey,
                    konachanApiKey = settings.serviceApiConfig.konachan.apiKey,
                    konachanUsername = settings.serviceApiConfig.konachan.username,
                    konachanPassword = settings.serviceApiConfig.konachan.password,
                    konachanEmail = settings.serviceApiConfig.konachan.email,
                    aiBaseUrl = settings.serviceApiConfig.ai.baseUrl,
                    aiApiKey = settings.serviceApiConfig.ai.apiKey,
                    aiModel = settings.serviceApiConfig.ai.model,
                    proxyEnabled = settings.proxyConfig.enabled,
                    proxyType = settings.proxyConfig.type,
                    proxyHost = settings.proxyConfig.host,
                    proxyPort = settings.proxyConfig.port?.toString().orEmpty(),
                    proxyUsername = settings.proxyConfig.username,
                    proxyPassword = settings.proxyConfig.password,
                    cacheSizeBytes = ImageMemoryCache.sizeBytes() + MediaDiskCache.sizeBytes() + VideoPlaybackCache.sizeBytes(),
                    cacheLimitBytes = -1L,
                )
            }
        }
    }

    fun updateRule34UserId(value: String) = mutate { copy(rule34UserId = value, errorMessage = null, successMessage = null) }
    fun updateRule34ApiKey(value: String) = mutate { copy(rule34ApiKey = value, errorMessage = null, successMessage = null) }
    fun updateKonachanApiKey(value: String) = mutate { copy(konachanApiKey = value, errorMessage = null, successMessage = null) }
    fun updateKonachanUsername(value: String) = mutate { copy(konachanUsername = value, errorMessage = null, successMessage = null) }
    fun updateKonachanPassword(value: String) = mutate { copy(konachanPassword = value, errorMessage = null, successMessage = null) }
    fun updateKonachanEmail(value: String) = mutate { copy(konachanEmail = value, errorMessage = null, successMessage = null) }
    fun updateAiBaseUrl(value: String) = mutate { copy(aiBaseUrl = value, errorMessage = null, successMessage = null) }
    fun updateAiApiKey(value: String) = mutate { copy(aiApiKey = value, errorMessage = null, successMessage = null) }
    fun updateAiModel(value: String) = mutate { copy(aiModel = value, errorMessage = null, successMessage = null) }
    fun updateProxyEnabled(value: Boolean) = mutate { copy(proxyEnabled = value, errorMessage = null, successMessage = null) }
    fun updateProxyType(value: ProxyType) = mutate { copy(proxyType = value, errorMessage = null, successMessage = null) }
    fun updateProxyHost(value: String) = mutate { copy(proxyHost = value, errorMessage = null, successMessage = null) }
    fun updateProxyPort(value: String) = mutate { copy(proxyPort = value, errorMessage = null, successMessage = null) }
    fun updateProxyUsername(value: String) = mutate { copy(proxyUsername = value, errorMessage = null, successMessage = null) }
    fun updateProxyPassword(value: String) = mutate { copy(proxyPassword = value, errorMessage = null, successMessage = null) }

    fun refreshCacheStats() {
        _state.value = _state.value.copy(
            cacheSizeBytes = ImageMemoryCache.sizeBytes() + MediaDiskCache.sizeBytes() + VideoPlaybackCache.sizeBytes(),
            cacheLimitBytes = -1L,
        )
    }

    fun saveProxySettings() {
        val current = _state.value
        val proxyError = SettingsValidator.validateProxy(
            enabled = current.proxyEnabled,
            host = current.proxyHost,
            portText = current.proxyPort,
        )
        if (proxyError != null) {
            _state.value = current.copy(errorMessage = proxyError, successMessage = null)
            return
        }

        viewModelScope.launch {
            runCatching {
                settingsRepository.updateProxyConfig(
                    ProxyConfig(
                        enabled = current.proxyEnabled,
                        type = current.proxyType,
                        host = current.proxyHost.trim(),
                        port = current.proxyPort.toIntOrNull(),
                        username = current.proxyUsername.trim(),
                        password = current.proxyPassword,
                    ),
                )
            }.onSuccess {
                hasPendingChanges = false
                _state.value = current.copy(
                    successMessage = "Прокси сохранён.",
                    errorMessage = null,
                )
            }.onFailure { error ->
                _state.value = current.copy(
                    errorMessage = error.message ?: "Не удалось сохранить прокси на сервере.",
                    successMessage = null,
                )
            }
        }
    }

    fun saveApiSettings() {
        val current = _state.value

        viewModelScope.launch {
            runCatching {
                settingsRepository.updateServiceApiConfig(
                    ServiceApiConfig(
                        rule34 = Rule34ApiConfig(
                            userId = current.rule34UserId.trim(),
                            apiKey = current.rule34ApiKey.trim(),
                        ),
                        konachan = KonachanApiConfig(
                            apiKey = current.konachanApiKey.trim(),
                            username = current.konachanUsername.trim(),
                            password = current.konachanPassword,
                            email = current.konachanEmail.trim(),
                        ),
                        ai = AiApiConfig(
                            baseUrl = current.aiBaseUrl.trim().removeSuffix("/"),
                            apiKey = current.aiApiKey.trim(),
                            model = current.aiModel.trim(),
                        ),
                    ),
                )
            }.onSuccess {
                hasPendingChanges = false
                _state.value = current.copy(
                    successMessage = "API-настройки сохранены.",
                    errorMessage = null,
                )
            }.onFailure { error ->
                _state.value = current.copy(
                    errorMessage = error.message ?: "Не удалось сохранить API-настройки на сервере.",
                    successMessage = null,
                )
            }
        }
    }

    fun clearMediaCache() {
        ImageMemoryCache.clear()
        MediaDiskCache.clear()
        VideoPlaybackCache.clear()
        _state.value = _state.value.copy(
            cacheSizeBytes = 0L,
            cacheLimitBytes = -1L,
            errorMessage = null,
            successMessage = "Кеш медиа очищен.",
        )
    }

    private inline fun mutate(transform: SettingsFormState.() -> SettingsFormState) {
        hasPendingChanges = true
        _state.value = _state.value.transform()
    }
}

object AppViewModelProvider {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { AppViewModel() }
        initializer {
            SearchViewModel(
                postsRepository = r34Application().container.postsRepository,
                favoritesRepository = r34Application().container.favoritesRepository,
                savedSearchRepository = r34Application().container.savedSearchRepository,
                preferencesRepository = r34Application().container.preferencesRepository,
                settingsRepository = r34Application().container.settingsRepository,
                aiTagResolver = r34Application().container.aiTagResolver,
            )
        }
        initializer {
            FavoritesViewModel(
                favoritesRepository = r34Application().container.favoritesRepository,
            )
        }
        initializer {
            SavedSearchesViewModel(
                savedSearchRepository = r34Application().container.savedSearchRepository,
            )
        }
        initializer {
            PreferencesViewModel(
                settingsRepository = r34Application().container.settingsRepository,
                preferencesRepository = r34Application().container.preferencesRepository,
            )
        }
        initializer {
            SettingsViewModel(
                settingsRepository = r34Application().container.settingsRepository,
            )
        }
    }
}

private fun CreationExtras.r34Application(): R34Application {
    return this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as R34Application
}
