package com.naviify.app.ui.connect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.naviify.app.R
import com.naviify.app.core.storage.ServerMode
import com.naviify.app.data.repository.ServerInfo
import com.naviify.app.ui.theme.NaviifyBlack
import com.naviify.app.ui.theme.SpotifyGreen
import com.naviify.app.ui.theme.TextPrimary
import com.naviify.app.ui.theme.TextSecondary

@Composable
fun ConnectScreen(viewModel: ConnectViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(containerColor = NaviifyBlack) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val current = state) {
                ConnectUiState.Loading -> CircularProgressIndicator(color = SpotifyGreen)
                is ConnectUiState.Form -> ConnectForm(
                    form = current,
                    enabled = true,
                    onFieldChange = { key, value ->
                        when (key) {
                            Field.ServerUrl -> viewModel.onServerUrlChange(value)
                            Field.RemoteUrl -> viewModel.onRemoteUrlChange(value)
                            Field.Username -> viewModel.onUsernameChange(value)
                            Field.Secret -> viewModel.onSecretChange(value)
                        }
                    },
                    onUseTokenChange = viewModel::onUseTokenChange,
                    onConnect = viewModel::connect,
                )
                is ConnectUiState.Checking -> ConnectForm(
                    form = current.form,
                    enabled = false,
                    onFieldChange = { _, _ -> },
                    onUseTokenChange = {},
                    onConnect = {},
                )
                is ConnectUiState.Connected -> ConnectedView(current, viewModel)
                is ConnectUiState.Failed -> FailedView(current, viewModel)
            }
        }
    }
}

private enum class Field { ServerUrl, RemoteUrl, Username, Secret }

@Composable
private fun ConnectForm(
    form: ConnectUiState.Form,
    enabled: Boolean,
    onFieldChange: (Field, String) -> Unit,
    onUseTokenChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "ASAYS Logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp)),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.connect_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Text(
            text = stringResource(R.string.connect_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = form.serverUrl,
            onValueChange = { onFieldChange(Field.ServerUrl, it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.server_url_label)) },
            placeholder = { Text(stringResource(R.string.server_url_hint), color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = connectFieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.username,
            onValueChange = { onFieldChange(Field.Username, it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = connectFieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.remoteUrl,
            onValueChange = { onFieldChange(Field.RemoteUrl, it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Remote / Tailscale URL") },
            placeholder = { Text("https://tailscale-…", color = TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = connectFieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = form.useToken,
                onClick = { onUseTokenChange(!form.useToken) },
                enabled = enabled,
                label = { Text(stringResource(R.string.use_token_label)) },
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = form.secret,
            onValueChange = { onFieldChange(Field.Secret, it) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.secret_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = connectFieldColors(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onConnect,
            enabled = enabled && form.serverUrl.isNotBlank() && form.username.isNotBlank() && form.secret.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyGreen,
                contentColor = androidx.compose.ui.graphics.Color.Black,
            ),
        ) {
            if (!enabled) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = androidx.compose.ui.graphics.Color.Black,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.checking_connection))
            } else {
                Text(stringResource(R.string.connect_button))
            }
        }
    }
}

@Composable
private fun ConnectedView(state: ConnectUiState.Connected, viewModel: ConnectViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = SpotifyGreen,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.connected_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.connected_to, state.serverUrl),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Icon(
                imageVector = if (state.serverMode == ServerMode.HOME) Icons.Rounded.Home else Icons.Rounded.Public,
                contentDescription = null,
                tint = SpotifyGreen,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = if (state.serverMode == ServerMode.HOME) "Home LAN" else "Tailscale",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            TextButton(onClick = viewModel::switchServerMode) {
                Text("Switch")
            }
        }
        state.serverInfo?.let { info ->
            Spacer(Modifier.height(4.dp))
            Text(
                text = serverInfoLabel(info),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(
            onClick = viewModel::editServer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.edit_server_button))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = viewModel::disconnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.disconnect_button),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun FailedView(state: ConnectUiState.Failed, viewModel: ConnectViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.connection_failed),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = viewModel::retry,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpotifyGreen,
                contentColor = androidx.compose.ui.graphics.Color.Black,
            ),
        ) {
            Text(stringResource(R.string.retry_button))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = viewModel::editServer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.edit_server_button))
        }
    }
}

@Composable
private fun serverInfoLabel(info: ServerInfo): String {
    val serverName = info.type ?: "Subsonic"
    val version = info.serverVersion ?: info.version
    return if (version != null) {
        stringResource(R.string.server_info, serverName, version)
    } else {
        serverName
    }
}

@Composable
private fun connectFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = SpotifyGreen,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = SpotifyGreen,
    unfocusedLabelColor = TextSecondary,
    cursorColor = SpotifyGreen,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledTextColor = TextSecondary,
)
