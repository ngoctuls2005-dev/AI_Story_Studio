package com.hoangtu.aistorystudio.core.ai.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.hoangtu.aistorystudio.core.security.EncryptedApiKey
import com.hoangtu.aistorystudio.core.security.SecureKeyStore
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreError
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DataStoreAiProviderSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val secureKeyStore: SecureKeyStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AiProviderSettingsRepository {
    override fun observeProviders(): Flow<List<AiProviderProfile>> =
        dataStore.data.map { preferences ->
            withContext(ioDispatcher) {
                val providerIds = preferences[PROVIDER_IDS].orEmpty()
                providerIds.mapNotNull { providerId ->
                    val configuration = readConfiguration(preferences, providerId) ?: return@mapNotNull null
                    val encryptedApiKey = readEncryptedApiKey(preferences, providerId)
                    val hasKeyFields = hasStoredApiKeyFields(preferences, providerId)
                    val credentialStatus = when {
                        !hasKeyFields -> CredentialStatus.MISSING
                        encryptedApiKey == null -> CredentialStatus.REENTRY_REQUIRED
                        else -> when (val result = secureKeyStore.decrypt(providerId, encryptedApiKey)) {
                            is SecureKeyStoreResult.Success -> {
                                result.value.fill(0)
                                CredentialStatus.AVAILABLE
                            }

                            is SecureKeyStoreResult.Failure -> CredentialStatus.REENTRY_REQUIRED
                        }
                    }
                    val validation = AiProviderConfigurationValidator.validate(configuration)
                    val localStatus = when {
                        !validation.isValid -> LocalConfigurationStatus.INVALID
                        credentialStatus == CredentialStatus.MISSING ->
                            LocalConfigurationStatus.MISSING_API_KEY

                        credentialStatus == CredentialStatus.REENTRY_REQUIRED ->
                            LocalConfigurationStatus.REENTRY_REQUIRED

                        else -> LocalConfigurationStatus.READY
                    }
                    AiProviderProfile(
                        configuration = configuration,
                        credentialStatus = credentialStatus,
                        localStatus = localStatus,
                        isActive = preferences[ACTIVE_PROVIDER_ID] == providerId
                    )
                }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.configuration.displayName })
            }
        }

    override suspend fun ensureDefaultProvider() {
        withContext(ioDispatcher) {
            dataStore.edit { preferences ->
                val providerIds = preferences[PROVIDER_IDS].orEmpty().toMutableSet()
                if (DefaultAiProviderConfiguration.PROVIDER_ID !in providerIds) {
                    providerIds += DefaultAiProviderConfiguration.PROVIDER_ID
                    writeConfiguration(preferences, DefaultAiProviderConfiguration.value)
                }
                preferences[PROVIDER_IDS] = providerIds
                val activeProviderId = preferences[ACTIVE_PROVIDER_ID]
                if (activeProviderId == null || activeProviderId !in providerIds) {
                    preferences[ACTIVE_PROVIDER_ID] = DefaultAiProviderConfiguration.PROVIDER_ID
                }
            }
        }
    }

    override suspend fun saveProvider(
        configuration: AiProviderConfiguration,
        apiKey: ByteArray?
    ): SaveProviderResult = withContext(ioDispatcher) {
        val validation = AiProviderConfigurationValidator.validate(configuration)
        if (!validation.isValid) return@withContext SaveProviderResult.Invalid(validation.errors)

        val replacement = if (apiKey == null || apiKey.isEmpty()) {
            null
        } else {
            when (val result = secureKeyStore.encrypt(configuration.providerId, apiKey)) {
                is SecureKeyStoreResult.Success -> result.value
                is SecureKeyStoreResult.Failure -> {
                    return@withContext SaveProviderResult.EncryptionFailed(result.error)
                }
            }
        }

        var previousKeyAlias: String? = null
        try {
            dataStore.edit { preferences ->
                val providerIds = preferences[PROVIDER_IDS].orEmpty().toMutableSet()
                providerIds += configuration.providerId
                preferences[PROVIDER_IDS] = providerIds
                writeConfiguration(preferences, configuration)
                if (replacement != null) {
                    previousKeyAlias = preferences[key(configuration.providerId, "key_alias")]
                    writeEncryptedApiKey(preferences, configuration.providerId, replacement)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withContext SaveProviderResult.StorageUnavailable
        }

        val oldKeyCleanupPending = replacement?.let { newKey ->
            previousKeyAlias?.takeIf { it != newKey.keyAlias }?.let { oldAlias ->
                secureKeyStore.deleteKey(oldAlias) is SecureKeyStoreResult.Failure
            }
        } ?: false
        SaveProviderResult.Saved(oldKeyCleanupPending)
    }

    override suspend fun selectProvider(providerId: String): Boolean = withContext(ioDispatcher) {
        var selected = false
        dataStore.edit { preferences ->
            if (providerId in preferences[PROVIDER_IDS].orEmpty()) {
                preferences[ACTIVE_PROVIDER_ID] = providerId
                selected = true
            }
        }
        selected
    }

    override suspend fun deleteApiKey(providerId: String): DeleteApiKeyResult = withContext(ioDispatcher) {
        var keyAlias: String? = null
        try {
            dataStore.edit { preferences ->
                keyAlias = preferences[key(providerId, "key_alias")]
                preferences.remove(key(providerId, "key_alias"))
                preferences.remove(key(providerId, "key_iv"))
                preferences.remove(key(providerId, "key_ciphertext"))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withContext DeleteApiKeyResult.StorageUnavailable
        }

        val deletedAlias = keyAlias ?: return@withContext DeleteApiKeyResult.Deleted
        when (secureKeyStore.deleteKey(deletedAlias)) {
            is SecureKeyStoreResult.Success -> DeleteApiKeyResult.Deleted
            is SecureKeyStoreResult.Failure -> DeleteApiKeyResult.DeletedWithKeyCleanupPending
        }
    }

    override suspend fun readApiKey(providerId: String): ApiKeyReadResult = withContext(ioDispatcher) {
        val preferences = try {
            dataStore.data.first()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withContext ApiKeyReadResult.StorageUnavailable
        }
        val encryptedApiKey = readEncryptedApiKey(preferences, providerId)
        if (!hasStoredApiKeyFields(preferences, providerId)) {
            return@withContext ApiKeyReadResult.Missing
        }
        if (encryptedApiKey == null) {
            return@withContext ApiKeyReadResult.NeedsReentry(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        }

        when (val result = secureKeyStore.decrypt(providerId, encryptedApiKey)) {
            is SecureKeyStoreResult.Success -> ApiKeyReadResult.Available(result.value)
            is SecureKeyStoreResult.Failure -> ApiKeyReadResult.NeedsReentry(result.error)
        }
    }

    override suspend fun validateLocally(providerId: String): LocalConfigurationStatus {
        val profile = try {
            observeProviders().first().firstOrNull {
                it.configuration.providerId == providerId
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return LocalConfigurationStatus.STORAGE_UNAVAILABLE
        }
        return profile?.localStatus ?: LocalConfigurationStatus.INVALID
    }

    private fun readConfiguration(
        preferences: Preferences,
        providerId: String
    ): AiProviderConfiguration? {
        val displayName = preferences[key(providerId, "display_name")] ?: return null
        val baseUrl = preferences[key(providerId, "base_url")] ?: return null
        val modelId = preferences[key(providerId, "model_id")] ?: return null
        val parameters = AiGenerationParameters(
            temperature = preferences[floatKey(providerId, "temperature")],
            topP = preferences[floatKey(providerId, "top_p")],
            maxOutputTokens = preferences[intKey(providerId, "max_output_tokens")]
        )
        return AiProviderConfiguration(
            providerId = providerId,
            displayName = displayName,
            baseUrl = baseUrl,
            modelId = modelId,
            parameters = parameters,
            allowInsecureHttp = preferences[booleanKey(providerId, "allow_insecure_http")] ?: false
        )
    }

    private fun writeConfiguration(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        configuration: AiProviderConfiguration
    ) {
        val providerId = configuration.providerId
        preferences[key(providerId, "display_name")] = configuration.displayName.trim()
        preferences[key(providerId, "base_url")] = configuration.baseUrl.trim().trimEnd('/')
        preferences[key(providerId, "model_id")] = configuration.modelId.trim()
        preferences[booleanKey(providerId, "allow_insecure_http")] = configuration.allowInsecureHttp
        writeOptional(preferences, floatKey(providerId, "temperature"), configuration.parameters.temperature)
        writeOptional(preferences, floatKey(providerId, "top_p"), configuration.parameters.topP)
        writeOptional(preferences, intKey(providerId, "max_output_tokens"), configuration.parameters.maxOutputTokens)
    }

    private fun writeEncryptedApiKey(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        providerId: String,
        encryptedApiKey: EncryptedApiKey
    ) {
        preferences[key(providerId, "key_alias")] = encryptedApiKey.keyAlias
        preferences[bytesKey(providerId, "key_iv")] = encryptedApiKey.initializationVector.copyOf()
        preferences[bytesKey(providerId, "key_ciphertext")] = encryptedApiKey.ciphertext.copyOf()
    }

    private fun readEncryptedApiKey(
        preferences: Preferences,
        providerId: String
    ): EncryptedApiKey? {
        val alias = preferences[key(providerId, "key_alias")] ?: return null
        val iv = preferences[bytesKey(providerId, "key_iv")] ?: return null
        val ciphertext = preferences[bytesKey(providerId, "key_ciphertext")] ?: return null
        return EncryptedApiKey(alias, iv.copyOf(), ciphertext.copyOf())
            .takeIf(EncryptedApiKey::isUsablePayload)
    }

    private fun hasStoredApiKeyFields(preferences: Preferences, providerId: String): Boolean =
        preferences.contains(key(providerId, "key_alias")) ||
            preferences.contains(bytesKey(providerId, "key_iv")) ||
            preferences.contains(bytesKey(providerId, "key_ciphertext"))

    private fun <T : Any> writeOptional(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        preferenceKey: androidx.datastore.preferences.core.Preferences.Key<T>,
        value: T?
    ) {
        if (value == null) preferences.remove(preferenceKey) else preferences[preferenceKey] = value
    }

    private companion object {
        val PROVIDER_IDS = stringSetPreferencesKey("provider_ids")
        val ACTIVE_PROVIDER_ID = stringPreferencesKey("active_provider_id")

        fun key(providerId: String, field: String) =
            stringPreferencesKey("provider.$providerId.$field")

        fun booleanKey(providerId: String, field: String) =
            booleanPreferencesKey("provider.$providerId.$field")

        fun floatKey(providerId: String, field: String) =
            floatPreferencesKey("provider.$providerId.$field")

        fun intKey(providerId: String, field: String) =
            intPreferencesKey("provider.$providerId.$field")

        fun bytesKey(providerId: String, field: String) =
            byteArrayPreferencesKey("provider.$providerId.$field")
    }
}
