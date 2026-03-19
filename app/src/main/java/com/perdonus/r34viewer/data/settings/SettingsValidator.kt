package com.perdonus.r34viewer.data.settings

object SettingsValidator {
    fun validateProxy(
        enabled: Boolean,
        host: String,
        portText: String,
    ): String? {
        if (!enabled) return null
        if (host.isBlank()) {
            return "Для proxy нужен host."
        }
        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) {
            return "Укажите корректный proxy port."
        }
        return null
    }
}
