package com.perdonus.r34viewer.data.settings

import com.perdonus.r34viewer.data.model.BooruService

enum class ProxyType {
    HTTP,
    SOCKS,
}

data class ProxyConfig(
    val enabled: Boolean = false,
    val type: ProxyType = ProxyType.HTTP,
    val host: String = "",
    val port: Int? = null,
    val username: String = "",
    val password: String = "",
) {
    val hasCredentials: Boolean
        get() = username.isNotBlank() && password.isNotBlank()

    fun signature(): String = listOf(
        enabled.toString(),
        type.name,
        host,
        port?.toString().orEmpty(),
        username,
        password,
    ).joinToString("|")
}

data class Rule34ApiConfig(
    val userId: String = "",
    val apiKey: String = "",
)

data class KonachanApiConfig(
    val apiKey: String = "",
    val username: String = "",
    val password: String = "",
    val email: String = "",
)

data class AiApiConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
)

data class ContentPreferences(
    val preferredTags: List<String> = emptyList(),
    val blockedTags: List<String> = emptyList(),
)

data class PreferenceCatalogItem(
    val tag: String,
    val titleRu: String,
    val postCount: Int,
)

data class ServiceApiConfig(
    val rule34: Rule34ApiConfig = Rule34ApiConfig(),
    val konachan: KonachanApiConfig = KonachanApiConfig(),
    val ai: AiApiConfig = AiApiConfig(),
)

data class AppSettings(
    val selectedService: BooruService = BooruService.RULE34,
    val hideAiContent: Boolean = false,
    val proxyConfig: ProxyConfig = ProxyConfig(),
    val serviceApiConfig: ServiceApiConfig = ServiceApiConfig(),
    val preferences: ContentPreferences = ContentPreferences(),
    val preferenceCatalog: List<PreferenceCatalogItem> = emptyList(),
    val preferenceTitles: Map<String, String> = emptyMap(),
)
