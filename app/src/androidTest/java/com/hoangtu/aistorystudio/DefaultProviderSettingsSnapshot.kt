package com.hoangtu.aistorystudio

import com.hoangtu.aistorystudio.core.ai.settings.AiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.AiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.ai.settings.ApiKeyReadResult
import com.hoangtu.aistorystudio.core.ai.settings.DefaultAiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.DeleteApiKeyResult
import com.hoangtu.aistorystudio.core.ai.settings.SaveProviderResult
import kotlinx.coroutines.flow.first

internal class DefaultProviderSettingsSnapshot private constructor(
    private val configuration: AiProviderConfiguration,
    private val apiKey: ByteArray?
) {
    suspend fun restore(repository: AiProviderSettingsRepository) {
        try {
            val deleted = repository.deleteApiKey(configuration.providerId)
            check(deleted is DeleteApiKeyResult.Deleted || deleted is DeleteApiKeyResult.DeletedWithKeyCleanupPending) {
                "Khong the khoi phuc cau hinh provider sau kiem thu."
            }
            val restoreKey = apiKey?.copyOf()
            val saved = try {
                repository.saveProvider(configuration, restoreKey)
            } finally {
                restoreKey?.fill(0)
            }
            check(saved is SaveProviderResult.Saved) {
                "Khong the khoi phuc cau hinh provider sau kiem thu."
            }
        } finally {
            apiKey?.fill(0)
        }
    }

    companion object {
        suspend fun capture(repository: AiProviderSettingsRepository): DefaultProviderSettingsSnapshot {
            repository.ensureDefaultProvider()
            val profile = repository.observeProviders().first().firstOrNull {
                it.configuration.providerId == DefaultAiProviderConfiguration.PROVIDER_ID
            } ?: error("Khong tim thay cau hinh provider mac dinh.")

            val key = when (val result = repository.readApiKey(DefaultAiProviderConfiguration.PROVIDER_ID)) {
                is ApiKeyReadResult.Available -> result.value
                ApiKeyReadResult.Missing -> null
                else -> error("Khong the doc an toan khoa provider truoc kiem thu.")
            }
            return DefaultProviderSettingsSnapshot(profile.configuration, key)
        }
    }
}
