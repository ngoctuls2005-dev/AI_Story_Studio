package com.hoangtu.aistorystudio.core.ai.settings

import com.hoangtu.aistorystudio.core.security.EncryptedApiKey
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreError
import kotlinx.coroutines.flow.Flow

interface AiProviderSettingsRepository {
    fun observeProviders(): Flow<List<AiProviderProfile>>
    suspend fun ensureDefaultProvider()
    suspend fun saveProvider(
        configuration: AiProviderConfiguration,
        apiKey: ByteArray?
    ): SaveProviderResult
    suspend fun selectProvider(providerId: String): Boolean
    suspend fun deleteApiKey(providerId: String): DeleteApiKeyResult
    suspend fun readApiKey(providerId: String): ApiKeyReadResult
    suspend fun validateLocally(providerId: String): LocalConfigurationStatus
}

sealed interface SaveProviderResult {
    data class Saved(val oldKeyCleanupPending: Boolean = false) : SaveProviderResult
    data class Invalid(val errors: Set<AiProviderFieldError>) : SaveProviderResult
    data class EncryptionFailed(val error: SecureKeyStoreError) : SaveProviderResult
    data object StorageUnavailable : SaveProviderResult
}

sealed interface DeleteApiKeyResult {
    data object Deleted : DeleteApiKeyResult
    data object DeletedWithKeyCleanupPending : DeleteApiKeyResult
    data object StorageUnavailable : DeleteApiKeyResult
}

sealed interface ApiKeyReadResult {
    data class Available(val value: ByteArray) : ApiKeyReadResult
    data object Missing : ApiKeyReadResult
    data class NeedsReentry(val reason: SecureKeyStoreError) : ApiKeyReadResult
    data object StorageUnavailable : ApiKeyReadResult
}

internal fun EncryptedApiKey.isUsablePayload(): Boolean =
    keyAlias.isNotBlank() && initializationVector.isNotEmpty() && ciphertext.isNotEmpty()
