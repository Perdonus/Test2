package com.perdonus.r34viewer.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.perdonus.r34viewer.data.model.BooruService
import com.perdonus.r34viewer.data.remote.RuleServerStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun updateAiFilter(enabled: Boolean)

    suspend fun updateSelectedService(service: BooruService)

    suspend fun updateProxyConfig(proxyConfig: ProxyConfig)

    suspend fun updateServiceApiConfig(serviceApiConfig: ServiceApiConfig)

    suspend fun updateServerSettings(
        proxyConfig: ProxyConfig,
        serviceApiConfig: ServiceApiConfig,
    )
}

private val Context.dataStore by preferencesDataStore(name = "r34_settings")

class SettingsRepositoryImpl(
    private val context: Context,
    private val ruleServerStore: RuleServerStore,
) : SettingsRepository {

    private val localSettings: Flow<LocalSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::toLocalSettings)

    override val settings: Flow<AppSettings> = combine(localSettings, ruleServerStore.state) { local, remote ->
        AppSettings(
            selectedService = local.selectedService,
            hideAiContent = local.hideAiContent,
            proxyConfig = remote.proxyConfig,
            serviceApiConfig = remote.serviceApiConfig,
            preferences = remote.preferences,
            preferenceCatalog = remote.preferenceCatalog,
            preferenceTitles = remote.preferenceTitles,
        )
    }.distinctUntilChanged()

    override suspend fun updateSelectedService(service: BooruService) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SelectedService] = service.id
        }
    }

    override suspend fun updateAiFilter(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HideAiContent] = enabled
        }
    }

    override suspend fun updateProxyConfig(proxyConfig: ProxyConfig) {
        ruleServerStore.updateProxy(proxyConfig)
    }

    override suspend fun updateServiceApiConfig(serviceApiConfig: ServiceApiConfig) {
        ruleServerStore.updateServiceApiConfig(serviceApiConfig)
    }

    override suspend fun updateServerSettings(
        proxyConfig: ProxyConfig,
        serviceApiConfig: ServiceApiConfig,
    ) {
        ruleServerStore.updateServerSettings(
            proxyConfig = proxyConfig,
            serviceApiConfig = serviceApiConfig,
        )
    }

    private fun toLocalSettings(preferences: Preferences): LocalSettings {
        return LocalSettings(
            selectedService = BooruService.fromId(preferences[Keys.SelectedService]).asSelectableOrDefault(),
            hideAiContent = preferences[Keys.HideAiContent] ?: false,
        )
    }

    private object Keys {
        val SelectedService = stringPreferencesKey("selected_service")
        val HideAiContent = booleanPreferencesKey("hide_ai_content")
    }
}

private data class LocalSettings(
    val selectedService: BooruService = BooruService.RULE34,
    val hideAiContent: Boolean = false,
)
