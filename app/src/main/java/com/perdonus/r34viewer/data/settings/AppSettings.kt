package com.perdonus.r34viewer.data.settings

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

data class AppSettings(
    val apiUserId: String = "",
    val apiKey: String = "",
    val hideAiContent: Boolean = false,
    val proxyConfig: ProxyConfig = ProxyConfig(),
) {
    val hasApiCredentials: Boolean
        get() = apiUserId.isNotBlank() && apiKey.isNotBlank()
}
