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
import com.perdonus.r34viewer.data.local.SavedSearchEntity
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.model.Rule34Post
import com.perdonus.r34viewer.data.remote.AiTagResolver
import com.perdonus.r34viewer.data.remote.AiTagResolverException
import com.perdonus.r34viewer.data.repository.FavoritesRepository
import com.perdonus.r34viewer.data.repository.PostsRepository
import com.perdonus.r34viewer.data.repository.SavedSearchRepository
import com.perdonus.r34viewer.data.settings.AppSettings
import com.perdonus.r34viewer.data.settings.ProxyConfig
import com.perdonus.r34viewer.data.settings.ProxyType
import com.perdonus.r34viewer.data.settings.SettingsRepository
import com.perdonus.r34viewer.data.settings.SettingsValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val settingsRepository: SettingsRepository,
    private val aiTagResolver: AiTagResolver,
) : ViewModel() {
    data class SearchRequest(
        val query: String = "",
        val nonce: Long = 0,
    )

    private val _queryText = MutableStateFlow("")
    val queryText = _queryText.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage = _feedbackMessage.asStateFlow()

    private val _isAiResolving = MutableStateFlow(false)
    val isAiResolving = _isAiResolving.asStateFlow()

    private val _searchRequest = MutableStateFlow(SearchRequest())

    val hasSubmittedSearch = _searchRequest
        .map { it.query.isNotBlank() }
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
        if (request.query.isBlank()) {
            flowOf(PagingData.empty<Rule34Post>())
        } else {
            postsRepository.search(request.query, appSettings)
        }
    }.cachedIn(viewModelScope)

    fun updateQuery(query: String) {
        _queryText.value = query
    }

    fun submitSearch() {
        val normalized = _queryText.value.trim()
        _queryText.value = normalized
        _feedbackMessage.value = null
        _searchRequest.value = SearchRequest(
            query = normalized,
            nonce = _searchRequest.value.nonce + 1,
        )
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
            _searchRequest.value = SearchRequest(
                query = normalized,
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
            val saved = savedSearchRepository.save(query, service)
            _feedbackMessage.value = if (saved) {
                "Запрос сохранён в ${service.displayName}."
            } else {
                "Такой запрос уже есть в закладках ${service.displayName}."
            }
        }
    }

    fun toggleFavorite(post: Rule34Post) {
        viewModelScope.launch {
            favoritesRepository.toggle(post)
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
            val activeQuery = _searchRequest.value.query
            if (activeQuery.isNotBlank()) {
                _searchRequest.value = SearchRequest(
                    query = activeQuery,
                    nonce = _searchRequest.value.nonce + 1,
                )
            }
        }
    }

    fun runAiSearch() {
        val rawQuery = _queryText.value.trim()
        if (rawQuery.isBlank() || _isAiResolving.value) return

        viewModelScope.launch {
            _isAiResolving.value = true
            try {
                val currentSettings = settings.value
                val resolution = aiTagResolver.resolve(
                    settings = currentSettings,
                    service = currentSettings.selectedService,
                    rawQuery = rawQuery,
                )
                _queryText.value = resolution.resolvedQuery
                _feedbackMessage.value = buildString {
                    append("AI-поиск: ${resolution.resolvedQuery}")
                    resolution.explanation?.takeIf { it.isNotBlank() }?.let { explanation ->
                        append(". ")
                        append(explanation)
                    }
                }
                _searchRequest.value = SearchRequest(
                    query = resolution.resolvedQuery,
                    nonce = _searchRequest.value.nonce + 1,
                )
            } catch (exception: AiTagResolverException) {
                _feedbackMessage.value = exception.message ?: "AI-поиск не сработал."
            } finally {
                _isAiResolving.value = false
            }
        }
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
            favoritesRepository.toggle(post)
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
            savedSearchRepository.delete(id)
        }
    }

    fun rename(id: Long, label: String) {
        viewModelScope.launch {
            savedSearchRepository.rename(id, label)
        }
    }
}

data class SettingsFormState(
    val isLoaded: Boolean = false,
    val proxyEnabled: Boolean = false,
    val proxyType: ProxyType = ProxyType.HTTP,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsFormState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            _state.value = SettingsFormState(
                isLoaded = true,
                proxyEnabled = settings.proxyConfig.enabled,
                proxyType = settings.proxyConfig.type,
                proxyHost = settings.proxyConfig.host,
                proxyPort = settings.proxyConfig.port?.toString().orEmpty(),
                proxyUsername = settings.proxyConfig.username,
                proxyPassword = settings.proxyConfig.password,
            )
        }
    }

    fun updateProxyEnabled(value: Boolean) = mutate { copy(proxyEnabled = value, errorMessage = null, successMessage = null) }
    fun updateProxyType(value: ProxyType) = mutate { copy(proxyType = value, errorMessage = null, successMessage = null) }
    fun updateProxyHost(value: String) = mutate { copy(proxyHost = value, errorMessage = null, successMessage = null) }
    fun updateProxyPort(value: String) = mutate { copy(proxyPort = value, errorMessage = null, successMessage = null) }
    fun updateProxyUsername(value: String) = mutate { copy(proxyUsername = value, errorMessage = null, successMessage = null) }
    fun updateProxyPassword(value: String) = mutate { copy(proxyPassword = value, errorMessage = null, successMessage = null) }

    fun save() {
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
            settingsRepository.updateProxy(
                ProxyConfig(
                    enabled = current.proxyEnabled,
                    type = current.proxyType,
                    host = current.proxyHost.trim(),
                    port = current.proxyPort.toIntOrNull(),
                    username = current.proxyUsername.trim(),
                    password = current.proxyPassword,
                ),
            )
            _state.value = current.copy(
                successMessage = "Proxy сохранён.",
                errorMessage = null,
            )
        }
    }

    private inline fun mutate(transform: SettingsFormState.() -> SettingsFormState) {
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
            SettingsViewModel(
                settingsRepository = r34Application().container.settingsRepository,
            )
        }
    }
}

private fun CreationExtras.r34Application(): R34Application {
    return this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as R34Application
}
