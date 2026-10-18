package com.hoangtu.aistorystudio.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangtu.aistorystudio.core.ai.settings.AiGenerationParameters
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderConfigurationValidator
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderFieldError
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderProfile
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.ai.settings.DeleteApiKeyResult
import com.hoangtu.aistorystudio.core.ai.settings.LocalConfigurationStatus
import com.hoangtu.aistorystudio.core.ai.settings.SaveProviderResult
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProviderEditorState(
    val providerId: String,
    val displayName: String,
    val baseUrl: String,
    val modelId: String,
    val temperature: String,
    val topP: String,
    val maxOutputTokens: String,
    val allowInsecureHttp: Boolean
) {
    companion object {
        fun from(configuration: AiProviderConfiguration) = ProviderEditorState(
            providerId = configuration.providerId,
            displayName = configuration.displayName,
            baseUrl = configuration.baseUrl,
            modelId = configuration.modelId,
            temperature = configuration.parameters.temperature?.toString().orEmpty(),
            topP = configuration.parameters.topP?.toString().orEmpty(),
            maxOutputTokens = configuration.parameters.maxOutputTokens?.toString().orEmpty(),
            allowInsecureHttp = configuration.allowInsecureHttp
        )
    }

    fun toConfiguration(): AiProviderConfiguration? {
        val parsedTemperature = temperature.toOptionalFloat() ?:
            if (temperature.isBlank()) null else return null
        val parsedTopP = topP.toOptionalFloat() ?: if (topP.isBlank()) null else return null
        val parsedMaxTokens = maxOutputTokens.toOptionalInt() ?:
            if (maxOutputTokens.isBlank()) null else return null
        return AiProviderConfiguration(
            providerId = providerId,
            displayName = displayName,
            baseUrl = baseUrl,
            modelId = modelId,
            parameters = AiGenerationParameters(
                temperature = parsedTemperature,
                topP = parsedTopP,
                maxOutputTokens = parsedMaxTokens
            ),
            allowInsecureHttp = allowInsecureHttp
        )
    }

    private fun String.toOptionalFloat(): Float? = takeIf(String::isNotBlank)?.toFloatOrNull()
    private fun String.toOptionalInt(): Int? = takeIf(String::isNotBlank)?.toIntOrNull()
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val profiles: List<AiProviderProfile> = emptyList(),
    val editor: ProviderEditorState? = null,
    val isSaving: Boolean = false,
    val pendingDeleteProviderId: String? = null,
    val message: String? = null,
    val isMessageError: Boolean = false
)

class SettingsViewModel(
    private val repository: AiProviderSettingsRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureDefaultProvider()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(isLoading = false, message = STORAGE_ERROR, isMessageError = true)
                }
                return@launch
            }
            repository.observeProviders()
                .catch {
                    mutableState.update {
                        it.copy(isLoading = false, message = STORAGE_ERROR, isMessageError = true)
                    }
                }
                .collect { profiles ->
                    mutableState.update { it.copy(isLoading = false, profiles = profiles) }
                }
        }
    }

    fun addProvider() {
        mutableState.update {
            it.copy(
                editor = ProviderEditorState(
                    providerId = UUID.randomUUID().toString(),
                    displayName = "Nhà cung cấp mới",
                    baseUrl = "https://",
                    modelId = "",
                    temperature = "",
                    topP = "",
                    maxOutputTokens = "",
                    allowInsecureHttp = false
                ),
                message = null,
                isMessageError = false
            )
        }
    }

    fun editProvider(profile: AiProviderProfile) {
        mutableState.update {
            it.copy(
                editor = ProviderEditorState.from(profile.configuration),
                message = null,
                isMessageError = false
            )
        }
    }

    fun updateEditor(transform: (ProviderEditorState) -> ProviderEditorState) {
        mutableState.update { current ->
            current.copy(editor = current.editor?.let(transform), message = null)
        }
    }

    fun closeEditor() {
        mutableState.update { it.copy(editor = null) }
    }

    fun saveProvider(apiKey: String) {
        val editor = mutableState.value.editor ?: return
        val configuration = editor.toConfiguration()
        if (configuration == null) {
            showError("Tham số temperature, top_p hoặc max tokens không hợp lệ.")
            return
        }
        val validation = AiProviderConfigurationValidator.validate(configuration)
        if (!validation.isValid) {
            showError(validation.errors.message())
            return
        }

        mutableState.update { it.copy(isSaving = true, message = null, isMessageError = false) }
        viewModelScope.launch {
            val apiKeyBytes = apiKey.takeUnless(String::isBlank)?.toByteArray(Charsets.UTF_8)
            val result = try {
                repository.saveProvider(configuration, apiKeyBytes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                SaveProviderResult.StorageUnavailable
            } finally {
                apiKeyBytes?.fill(0)
            }
            when (result) {
                is SaveProviderResult.Saved -> mutableState.update {
                    it.copy(
                        isSaving = false,
                        editor = null,
                        message = if (result.oldKeyCleanupPending) {
                            "Đã lưu cấu hình và khóa mới; còn khóa mã hóa cũ cần dọn dẹp."
                        } else if (apiKey.isBlank()) {
                            "Đã lưu cấu hình. Khóa API hiện có được giữ nguyên nếu đã lưu trước đó."
                        } else {
                            "Đã lưu cấu hình và khóa API an toàn."
                        },
                        isMessageError = false
                    )
                }

                is SaveProviderResult.Invalid -> showError(result.errors.message())
                is SaveProviderResult.EncryptionFailed -> showError(
                    "Không thể mã hóa khóa API bằng Android Keystore. Hãy thử lại hoặc nhập lại khóa."
                )

                SaveProviderResult.StorageUnavailable -> showError(STORAGE_ERROR)
            }
            mutableState.update { it.copy(isSaving = false) }
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            val selected = try {
                repository.selectProvider(providerId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (!selected) showError(STORAGE_ERROR)
        }
    }

    fun validateLocally(providerId: String) {
        viewModelScope.launch {
            val status = repository.validateLocally(providerId)
            val message = when (status) {
                LocalConfigurationStatus.READY -> "Cấu hình hợp lệ cục bộ; chưa gửi yêu cầu mạng."
                LocalConfigurationStatus.MISSING_API_KEY -> "Cấu hình hợp lệ nhưng chưa có API key."
                LocalConfigurationStatus.REENTRY_REQUIRED ->
                    "Không đọc được API key đã mã hóa. Hãy nhập lại khóa."

                LocalConfigurationStatus.STORAGE_UNAVAILABLE ->
                    "Không thể đọc cấu hình cục bộ trên thiết bị."

                LocalConfigurationStatus.INVALID -> "Cấu hình chưa hợp lệ. Hãy kiểm tra URL, model và tham số."
            }
            mutableState.update { it.copy(message = message, isMessageError = status != LocalConfigurationStatus.READY) }
        }
    }

    fun requestDeleteApiKey(providerId: String) {
        mutableState.update { it.copy(pendingDeleteProviderId = providerId) }
    }

    fun cancelDeleteApiKey() {
        mutableState.update { it.copy(pendingDeleteProviderId = null) }
    }

    fun confirmDeleteApiKey() {
        val providerId = mutableState.value.pendingDeleteProviderId ?: return
        viewModelScope.launch {
            val result = try {
                repository.deleteApiKey(providerId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                DeleteApiKeyResult.StorageUnavailable
            }
            val message = when (result) {
                DeleteApiKeyResult.Deleted -> "API key đã bị xóa."
                DeleteApiKeyResult.DeletedWithKeyCleanupPending ->
                    "Ciphertext đã bị xóa; Android Keystore chưa dọn được khóa mã hóa cũ."

                DeleteApiKeyResult.StorageUnavailable -> STORAGE_ERROR
            }
            mutableState.update {
                it.copy(
                    pendingDeleteProviderId = null,
                    message = message,
                    isMessageError = result == DeleteApiKeyResult.StorageUnavailable
                )
            }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null, isMessageError = false) }
    }

    private fun showError(message: String) {
        mutableState.update { it.copy(message = message, isMessageError = true, isSaving = false) }
    }

    private fun Set<AiProviderFieldError>.message(): String = when {
        AiProviderFieldError.INVALID_BASE_URL in this ->
            "Base URL không hợp lệ. Dùng HTTPS; HTTP chỉ được lưu khi bật xác nhận không an toàn."

        AiProviderFieldError.MISSING_DISPLAY_NAME in this -> "Tên provider không được để trống."
        AiProviderFieldError.MISSING_MODEL_ID in this -> "Model ID không được để trống."
        AiProviderFieldError.INVALID_TEMPERATURE in this -> "Temperature phải nằm trong khoảng 0 đến 2."
        AiProviderFieldError.INVALID_TOP_P in this -> "Top-p phải nằm trong khoảng 0 đến 1."
        AiProviderFieldError.INVALID_MAX_OUTPUT_TOKENS in this -> "Max output tokens phải lớn hơn 0."
        else -> "Thông tin provider chưa hợp lệ."
    }

    private companion object {
        const val STORAGE_ERROR = "Không thể đọc hoặc lưu cấu hình trên thiết bị."
    }
}
