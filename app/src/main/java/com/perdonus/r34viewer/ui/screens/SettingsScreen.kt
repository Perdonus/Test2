package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.data.settings.ProxyType
import com.perdonus.r34viewer.ui.components.ScreenHeader
import com.perdonus.r34viewer.ui.viewmodel.SettingsFormState

@Composable
fun SettingsScreen(
    state: SettingsFormState,
    onProxyEnabledChanged: (Boolean) -> Unit,
    onProxyTypeChanged: (ProxyType) -> Unit,
    onProxyHostChanged: (String) -> Unit,
    onProxyPortChanged: (String) -> Unit,
    onProxyUsernameChanged: (String) -> Unit,
    onProxyPasswordChanged: (String) -> Unit,
    onRule34UserIdChanged: (String) -> Unit,
    onRule34ApiKeyChanged: (String) -> Unit,
    onKonachanApiKeyChanged: (String) -> Unit,
    onKonachanUsernameChanged: (String) -> Unit,
    onKonachanPasswordChanged: (String) -> Unit,
    onKonachanEmailChanged: (String) -> Unit,
    onAiBaseUrlChanged: (String) -> Unit,
    onAiApiKeyChanged: (String) -> Unit,
    onAiModelChanged: (String) -> Unit,
    onRefreshCacheStats: () -> Unit,
    onClearImageCache: () -> Unit,
    onSave: () -> Unit,
) {
    LaunchedEffect(Unit) {
        onRefreshCacheStats()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Настройки")

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Прокси для booru", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "HTTP и SOCKS применяются только к загрузке постов и тегов. ИИ-резолв идёт через сервер напрямую и не использует этот прокси.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Использовать прокси", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = state.proxyEnabled,
                            onCheckedChange = onProxyEnabledChanged,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = state.proxyType == ProxyType.HTTP,
                            onClick = { onProxyTypeChanged(ProxyType.HTTP) },
                            label = { Text("HTTP") },
                        )
                        FilterChip(
                            selected = state.proxyType == ProxyType.SOCKS,
                            onClick = { onProxyTypeChanged(ProxyType.SOCKS) },
                            label = { Text("SOCKS") },
                        )
                    }
                    SettingsTextField(
                        value = state.proxyHost,
                        onValueChange = onProxyHostChanged,
                        label = "Хост",
                    )
                    SettingsTextField(
                        value = state.proxyPort,
                        onValueChange = onProxyPortChanged,
                        label = "Порт",
                    )
                    SettingsTextField(
                        value = state.proxyUsername,
                        onValueChange = onProxyUsernameChanged,
                        label = "Логин",
                    )
                    SettingsTextField(
                        value = state.proxyPassword,
                        onValueChange = onProxyPasswordChanged,
                        label = "Пароль",
                        secret = true,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Rule34 API", style = MaterialTheme.typography.titleMedium)
                    SettingsTextField(
                        value = state.rule34UserId,
                        onValueChange = onRule34UserIdChanged,
                        label = "User ID",
                    )
                    SettingsTextField(
                        value = state.rule34ApiKey,
                        onValueChange = onRule34ApiKeyChanged,
                        label = "API key",
                        secret = true,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Konachan API", style = MaterialTheme.typography.titleMedium)
                    SettingsTextField(
                        value = state.konachanApiKey,
                        onValueChange = onKonachanApiKeyChanged,
                        label = "API key",
                        secret = true,
                    )
                    SettingsTextField(
                        value = state.konachanUsername,
                        onValueChange = onKonachanUsernameChanged,
                        label = "Логин",
                    )
                    SettingsTextField(
                        value = state.konachanPassword,
                        onValueChange = onKonachanPasswordChanged,
                        label = "Пароль",
                        secret = true,
                    )
                    SettingsTextField(
                        value = state.konachanEmail,
                        onValueChange = onKonachanEmailChanged,
                        label = "E-mail",
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("ИИ-сервис", style = MaterialTheme.typography.titleMedium)
                    SettingsTextField(
                        value = state.aiBaseUrl,
                        onValueChange = onAiBaseUrlChanged,
                        label = "Base URL",
                    )
                    SettingsTextField(
                        value = state.aiApiKey,
                        onValueChange = onAiApiKeyChanged,
                        label = "API key",
                        secret = true,
                    )
                    SettingsTextField(
                        value = state.aiModel,
                        onValueChange = onAiModelChanged,
                        label = "Модель",
                    )
                }
            }

            state.errorMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.successMessage?.let { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Кеш изображений", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Занято: ${formatBytes(state.cacheSizeBytes)} из ${formatBytes(state.cacheLimitBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onClearImageCache,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Очистить кеш")
                    }
                }
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить на сервер")
            }
        }
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    secret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"

    val mb = bytes / 1024.0 / 1024.0
    return if (mb < 1024.0) {
        String.format("%.1f MB", mb)
    } else {
        String.format("%.2f GB", mb / 1024.0)
    }
}
