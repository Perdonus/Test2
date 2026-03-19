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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.data.settings.ProxyType
import com.perdonus.r34viewer.ui.viewmodel.SettingsFormState

@Composable
fun SettingsScreen(
    state: SettingsFormState,
    onApiUserIdChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onHideAiChanged: (Boolean) -> Unit,
    onProxyEnabledChanged: (Boolean) -> Unit,
    onProxyTypeChanged: (ProxyType) -> Unit,
    onProxyHostChanged: (String) -> Unit,
    onProxyPortChanged: (String) -> Unit,
    onProxyUsernameChanged: (String) -> Unit,
    onProxyPasswordChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CenterAlignedTopAppBar(title = { Text("Settings") })

        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                    Text("Rule34 API", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Получить user_id и api_key можно в настройках аккаунта rule34.xxx.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedTextField(
                        value = state.apiUserId,
                        onValueChange = onApiUserIdChanged,
                        label = { Text("user_id") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = onApiKeyChanged,
                        label = { Text("api_key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hide AI content", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Скрывает посты с тегами ai_generated и ai_assisted.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = state.hideAiContent,
                            onCheckedChange = onHideAiChanged,
                        )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Proxy", style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = state.proxyEnabled,
                            onCheckedChange = onProxyEnabledChanged,
                        )
                    }
                    Text(
                        "Поддерживаются HTTP и SOCKS. Настройки применяются ко всем сетевым запросам приложения.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
                    OutlinedTextField(
                        value = state.proxyHost,
                        onValueChange = onProxyHostChanged,
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.proxyPort,
                        onValueChange = onProxyPortChanged,
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.proxyUsername,
                        onValueChange = onProxyUsernameChanged,
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.proxyPassword,
                        onValueChange = onProxyPasswordChanged,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
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

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save settings")
            }
        }
    }
}
