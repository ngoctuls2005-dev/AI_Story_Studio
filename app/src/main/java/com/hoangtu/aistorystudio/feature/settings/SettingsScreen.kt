package com.hoangtu.aistorystudio.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderProfile
import com.hoangtu.aistorystudio.core.ai.settings.CredentialStatus
import com.hoangtu.aistorystudio.core.ai.settings.LocalConfigurationStatus
import com.hoangtu.aistorystudio.core.ui.SectionHeader
import com.hoangtu.aistorystudio.core.ui.StudioPage

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    StudioPage(
        testTag = "screen_settings",
        eyebrow = "TÙY CHỈNH",
        title = "Cài đặt",
        description = "Quản lý giao diện và cấu hình AI trên thiết bị."
    ) {
        SectionHeader(
            title = "Nhà cung cấp AI",
            description = "Cấu hình được lưu cục bộ. Ứng dụng chưa gửi yêu cầu kết nối ở màn hình này."
        )
        if (state.isLoading) {
            Text("Đang tải cấu hình…")
        } else {
            state.profiles.forEach { profile ->
                ProviderCard(
                    profile = profile,
                    onSelect = { viewModel.selectProvider(profile.configuration.providerId) },
                    onEdit = { viewModel.editProvider(profile) },
                    onValidate = { viewModel.validateLocally(profile.configuration.providerId) },
                    onDeleteKey = { viewModel.requestDeleteApiKey(profile.configuration.providerId) }
                )
            }
            OutlinedButton(
                onClick = viewModel::addProvider,
                modifier = Modifier.fillMaxWidth().testTag("provider_add")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Thêm provider", modifier = Modifier.padding(start = 8.dp))
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.isMessageError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("settings_message")
            )
        }

        state.editor?.let { editor ->
            ProviderEditor(
                editor = editor,
                isSaving = state.isSaving,
                onUpdate = viewModel::updateEditor,
                onSave = viewModel::saveProvider,
                onCancel = viewModel::closeEditor
            )
        }
    }

    val pendingDeleteProviderId = state.pendingDeleteProviderId
    if (pendingDeleteProviderId != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteApiKey,
            title = { Text("Xóa API key?") },
            text = { Text("Khóa đã mã hóa của provider này sẽ bị xóa khỏi thiết bị. Dữ liệu truyện không bị ảnh hưởng.") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmDeleteApiKey,
                    modifier = Modifier.testTag("api_key_confirm_delete")
                ) { Text("Xóa khóa") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteApiKey) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun ProviderCard(
    profile: AiProviderProfile,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onValidate: () -> Unit,
    onDeleteKey: () -> Unit
) {
    val configuration = profile.configuration
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = profile.isActive,
                    onClick = onSelect,
                    modifier = Modifier.testTag("provider_select_${configuration.providerId}")
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(configuration.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(configuration.modelId, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onEdit, modifier = Modifier.testTag("provider_edit_${configuration.providerId}")) {
                    Icon(Icons.Filled.Edit, contentDescription = "Chỉnh sửa ${configuration.displayName}")
                }
            }
            Text(configuration.baseUrl, style = MaterialTheme.typography.bodySmall)
            Text(credentialLabel(profile.credentialStatus), style = MaterialTheme.typography.labelLarge)
            Text(localStatusLabel(profile.localStatus), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onValidate) {
                    Text("Kiểm tra cục bộ")
                }
                if (profile.credentialStatus != CredentialStatus.MISSING) {
                    OutlinedButton(
                        onClick = onDeleteKey,
                        modifier = Modifier.testTag("api_key_delete_${configuration.providerId}")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Xóa key", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderEditor(
    editor: ProviderEditorState,
    isSaving: Boolean,
    onUpdate: ((ProviderEditorState) -> ProviderEditorState) -> Unit,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var apiKey by remember(editor.providerId) { mutableStateOf("") }
    var showApiKey by remember(editor.providerId) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().testTag("provider_editor")) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(
                title = "Cấu hình provider",
                description = "Provider ID ổn định: ${editor.providerId}"
            )
            OutlinedTextField(
                value = editor.displayName,
                onValueChange = { value -> onUpdate { it.copy(displayName = value) } },
                label = { Text("Tên hiển thị") },
                modifier = Modifier.fillMaxWidth().testTag("provider_name_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = editor.baseUrl,
                onValueChange = { value -> onUpdate { it.copy(baseUrl = value) } },
                label = { Text("Base URL") },
                modifier = Modifier.fillMaxWidth().testTag("provider_url_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = editor.modelId,
                onValueChange = { value -> onUpdate { it.copy(modelId = value) } },
                label = { Text("Model ID") },
                modifier = Modifier.fillMaxWidth().testTag("provider_model_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                placeholder = { Text("Để trống để giữ khóa đã lưu") },
                modifier = Modifier.fillMaxWidth().testTag("api_key_input"),
                singleLine = true,
                visualTransformation = if (showApiKey) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(
                        onClick = { showApiKey = !showApiKey },
                        modifier = Modifier.testTag("api_key_visibility_toggle")
                    ) {
                        Text(if (showApiKey) "Ẩn" else "Hiện")
                    }
                }
            )
            Text(
                text = "Khóa chỉ được dùng để tạo ciphertext; không hiển thị lại sau khi lưu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = editor.temperature,
                onValueChange = { value -> onUpdate { it.copy(temperature = value) } },
                label = { Text("Temperature (tùy chọn)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = editor.topP,
                onValueChange = { value -> onUpdate { it.copy(topP = value) } },
                label = { Text("Top-p (tùy chọn)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = editor.maxOutputTokens,
                onValueChange = { value -> onUpdate { it.copy(maxOutputTokens = value) } },
                label = { Text("Max output tokens (tùy chọn)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = editor.allowInsecureHttp,
                    onCheckedChange = { value -> onUpdate { it.copy(allowInsecureHttp = value) } },
                    modifier = Modifier.testTag("allow_insecure_http")
                )
                Text("Cho phép HTTP không mã hóa (không an toàn)")
            }
            Text(
                text = "HTTP bị từ chối mặc định. Chỉ bật nếu bạn hiểu rủi ro; API key có thể bị lộ trên mạng.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSave(apiKey) },
                    enabled = !isSaving,
                    modifier = Modifier.testTag("provider_save")
                ) { Text(if (isSaving) "Đang lưu…" else "Lưu cấu hình") }
                OutlinedButton(onClick = onCancel, enabled = !isSaving) { Text("Hủy") }
            }
        }
    }
}

private fun credentialLabel(status: CredentialStatus): String = when (status) {
    CredentialStatus.MISSING -> "Chưa có API key"
    CredentialStatus.AVAILABLE -> "Đã lưu khóa an toàn"
    CredentialStatus.REENTRY_REQUIRED -> "Không đọc được khóa — cần nhập lại"
}

private fun localStatusLabel(status: LocalConfigurationStatus): String = when (status) {
    LocalConfigurationStatus.READY -> "Cấu hình hợp lệ cục bộ; chưa kiểm tra mạng"
    LocalConfigurationStatus.MISSING_API_KEY -> "Thiếu API key"
    LocalConfigurationStatus.REENTRY_REQUIRED -> "Cần nhập lại API key"
    LocalConfigurationStatus.STORAGE_UNAVAILABLE -> "Không đọc được cấu hình cục bộ"
    LocalConfigurationStatus.INVALID -> "Cấu hình chưa hợp lệ"
}
