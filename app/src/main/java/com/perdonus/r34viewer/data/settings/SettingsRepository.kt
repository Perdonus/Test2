package com.perdonus.r34viewer.data.settings

import android.content.Context
import com.perdonus.r34viewer.data.model.BooruService
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun updateAiFilter(enabled: Boolean)

    suspend fun updateSelectedService(service: BooruService)

    suspend fun updateProxy(proxyConfig: ProxyConfig)
}

private val Context.dataStore by preferencesDataStore(name = "r34_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map(::toAppSettings)

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

    override suspend fun updateProxy(proxyConfig: ProxyConfig) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ProxyEnabled] = proxyConfig.enabled
            preferences[Keys.ProxyType] = proxyConfig.type.name
            preferences[Keys.ProxyHost] = proxyConfig.host
            proxyConfig.port?.let { preferences[Keys.ProxyPort] = it } ?: preferences.remove(Keys.ProxyPort)
            preferences[Keys.ProxyUsername] = proxyConfig.username
            preferences[Keys.ProxyPassword] = proxyConfig.password
        }
    }

    private fun toAppSettings(preferences: Preferences): AppSettings {
        val selectedService = BooruService.fromId(preferences[Keys.SelectedService])
        val proxyType = runCatching {
            ProxyType.valueOf(preferences[Keys.ProxyType] ?: ProxyType.HTTP.name)
        }.getOrDefault(ProxyType.HTTP)

        return AppSettings(
            selectedService = selectedService,
            hideAiContent = preferences[Keys.HideAiContent] ?: false,
            proxyConfig = ProxyConfig(
                enabled = preferences[Keys.ProxyEnabled] ?: false,
                type = proxyType,
                host = preferences[Keys.ProxyHost].orEmpty(),
                port = preferences[Keys.ProxyPort],
                username = preferences[Keys.ProxyUsername].orEmpty(),
                password = preferences[Keys.ProxyPassword].orEmpty(),
            ),
        )
    }

    private object Keys {
        val SelectedService = stringPreferencesKey("selected_service")
        val HideAiContent = booleanPreferencesKey("hide_ai_content")
        val ProxyEnabled = booleanPreferencesKey("proxy_enabled")
        val ProxyType = stringPreferencesKey("proxy_type")
        val ProxyHost = stringPreferencesKey("proxy_host")
        val ProxyPort = intPreferencesKey("proxy_port")
        val ProxyUsername = stringPreferencesKey("proxy_username")
        val ProxyPassword = stringPreferencesKey("proxy_password")
    }
}
