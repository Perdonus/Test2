package com.perdonus.r34viewer.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.perdonus.r34viewer.ui.components.ScreenHeader
import com.perdonus.r34viewer.ui.viewmodel.SettingsFormState

@Composable
fun ApiSettingsScreen(
    state: SettingsFormState,
    onBack: () -> Unit,
    onRule34UserIdChanged: (String) -> Unit,
    onRule34ApiKeyChanged: (String) -> Unit,
    onKonachanApiKeyChanged: (String) -> Unit,
    onKonachanUsernameChanged: (String) -> Unit,
    onKonachanPasswordChanged: (String) -> Unit,
    onKonachanEmailChanged: (String) -> Unit,
    onAiBaseUrlChanged: (String) -> Unit,
    onAiApiKeyChanged: (String) -> Unit,
    onAiModelChanged: (String) -> Unit,
    onSaveApi: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "API",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад")
                }
            },
        )

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
                    Text("Rule34 API", style = MaterialTheme.typography.titleMedium)
                    ApiSettingsTextField(
                        value = state.rule34UserId,
                        onValueChange = onRule34UserIdChanged,
                        label = "User ID",
                    )
                    ApiSettingsTextField(
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
                    ApiSettingsTextField(
                        value = state.konachanApiKey,
                        onValueChange = onKonachanApiKeyChanged,
                        label = "API key",
                        secret = true,
                    )
                    ApiSettingsTextField(
                        value = state.konachanUsername,
                        onValueChange = onKonachanUsernameChanged,
                        label = "Логин",
                    )
                    ApiSettingsTextField(
                        value = state.konachanPassword,
                        onValueChange = onKonachanPasswordChanged,
                        label = "Пароль",
                        secret = true,
                    )
                    ApiSettingsTextField(
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
                    ApiSettingsTextField(
                        value = state.aiBaseUrl,
                        onValueChange = onAiBaseUrlChanged,
                        label = "Base URL",
                    )
                    ApiSettingsTextField(
                        value = state.aiApiKey,
                        onValueChange = onAiApiKeyChanged,
                        label = "API key",
                        secret = true,
                    )
                    ApiSettingsTextField(
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

            Button(
                onClick = onSaveApi,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить API")
            }
        }
    }
}

@Composable
private fun ApiSettingsTextField(
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
