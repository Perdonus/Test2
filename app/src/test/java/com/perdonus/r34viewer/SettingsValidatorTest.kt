package com.perdonus.r34viewer

import com.perdonus.r34viewer.data.settings.SettingsValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsValidatorTest {
    @Test
    fun `requires proxy host when proxy enabled`() {
        assertEquals(
            "Для прокси нужен хост.",
            SettingsValidator.validateProxy(enabled = true, host = "", portText = "8080"),
        )
    }

    @Test
    fun `validates proxy port`() {
        assertEquals(
            "Укажите корректный порт прокси.",
            SettingsValidator.validateProxy(enabled = true, host = "127.0.0.1", portText = "99999"),
        )
    }

    @Test
    fun `accepts disabled proxy`() {
        assertNull(SettingsValidator.validateProxy(enabled = false, host = "", portText = ""))
    }
}
