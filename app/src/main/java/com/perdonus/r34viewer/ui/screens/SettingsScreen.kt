package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    onRefreshCacheStats: () -> Unit,
    onClearMediaCache: () -> Unit,
    onOpenApiSettings: () -> Unit,
    onOpenPreferences: () -> Unit,
    onSaveProxy: () -> Unit,
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
                    Text("API", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Rule34, Konachan и ИИ-ключи вынесены в отдельное меню, чтобы основной экран настроек не был перегружен.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onOpenApiSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Открыть API")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Предпочтения", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Отдельное меню для тегов, которые хочется видеть чаще или скрывать во всех поисках.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = onOpenPreferences,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Открыть предпочтения")
                    }
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
                    Text("Кеш медиа", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Занято: ${formatBytes(state.cacheSizeBytes)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("Лимит: без ограничений", style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = onClearMediaCache,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Очистить кеш")
                    }
                }
            }

            Button(
                onClick = onSaveProxy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить прокси")
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
