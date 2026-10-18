package com.hoangtu.aistorystudio.core.ai.settings

import java.net.URI

data class AiGenerationParameters(
    val temperature: Float? = null,
    val topP: Float? = null,
    val maxOutputTokens: Int? = null
)

data class AiProviderConfiguration(
    val providerId: String,
    val displayName: String,
    val baseUrl: String,
    val modelId: String,
    val parameters: AiGenerationParameters = AiGenerationParameters(),
    val allowInsecureHttp: Boolean = false
)

enum class CredentialStatus {
    MISSING,
    AVAILABLE,
    REENTRY_REQUIRED
}

enum class LocalConfigurationStatus {
    READY,
    MISSING_API_KEY,
    REENTRY_REQUIRED,
    STORAGE_UNAVAILABLE,
    INVALID
}

data class AiProviderProfile(
    val configuration: AiProviderConfiguration,
    val credentialStatus: CredentialStatus,
    val localStatus: LocalConfigurationStatus,
    val isActive: Boolean
)

enum class AiProviderFieldError {
    INVALID_PROVIDER_ID,
    MISSING_DISPLAY_NAME,
    INVALID_BASE_URL,
    MISSING_MODEL_ID,
    INVALID_TEMPERATURE,
    INVALID_TOP_P,
    INVALID_MAX_OUTPUT_TOKENS
}

data class AiProviderValidation(
    val errors: Set<AiProviderFieldError>
) {
    val isValid: Boolean get() = errors.isEmpty()
}

object AiProviderConfigurationValidator {
    fun validate(configuration: AiProviderConfiguration): AiProviderValidation {
        val errors = buildSet {
            if (!configuration.providerId.matches(Regex("[A-Za-z0-9_-]{1,64}"))) {
                add(AiProviderFieldError.INVALID_PROVIDER_ID)
            }
            if (configuration.displayName.isBlank()) {
                add(AiProviderFieldError.MISSING_DISPLAY_NAME)
            }
            if (!isBaseUrlAllowed(configuration.baseUrl, configuration.allowInsecureHttp)) {
                add(AiProviderFieldError.INVALID_BASE_URL)
            }
            if (configuration.modelId.isBlank()) {
                add(AiProviderFieldError.MISSING_MODEL_ID)
            }
            configuration.parameters.temperature?.let {
                if (!it.isFinite() || it !in 0f..2f) {
                    add(AiProviderFieldError.INVALID_TEMPERATURE)
                }
            }
            configuration.parameters.topP?.let {
                if (!it.isFinite() || it !in 0f..1f) {
                    add(AiProviderFieldError.INVALID_TOP_P)
                }
            }
            configuration.parameters.maxOutputTokens?.let {
                if (it <= 0) add(AiProviderFieldError.INVALID_MAX_OUTPUT_TOKENS)
            }
        }
        return AiProviderValidation(errors)
    }

    fun isBaseUrlAllowed(baseUrl: String, allowInsecureHttp: Boolean = false): Boolean {
        if (baseUrl.isBlank() || baseUrl != baseUrl.trim()) return false
        val uri = try {
            URI(baseUrl)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        val secureScheme = scheme == "https"
        val explicitlyAllowedHttp = scheme == "http" && allowInsecureHttp
        return uri.isAbsolute && uri.host != null && uri.rawUserInfo == null &&
            uri.rawQuery == null && uri.rawFragment == null &&
            (uri.port == -1 || uri.port in 1..65_535) &&
            (secureScheme || explicitlyAllowedHttp)
    }
}

object DefaultAiProviderConfiguration {
    const val PROVIDER_ID = "experiential-labs"

    val value = AiProviderConfiguration(
        providerId = PROVIDER_ID,
        displayName = "Experiential Labs",
        baseUrl = "https://api.experientiallabs.ai/v1",
        modelId = "deepseek-v4.1-flash-uncensored"
    )
}
