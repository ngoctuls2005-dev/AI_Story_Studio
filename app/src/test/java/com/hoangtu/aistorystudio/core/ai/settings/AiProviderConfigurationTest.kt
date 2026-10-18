package com.hoangtu.aistorystudio.core.ai.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiProviderConfigurationTest {
    @Test
    fun acceptsDefaultHttpsProviderAndModel() {
        val result = AiProviderConfigurationValidator.validate(DefaultAiProviderConfiguration.value)

        assertTrue(result.errors.toString(), result.isValid)
        assertTrue(DefaultAiProviderConfiguration.value.baseUrl.startsWith("https://"))
    }

    @Test
    fun rejectsInsecureHttpUnlessUserOptsIn() {
        val configuration = DefaultAiProviderConfiguration.value.copy(
            baseUrl = "http://localhost:8080/v1"
        )

        assertFalse(AiProviderConfigurationValidator.validate(configuration).isValid)
        assertTrue(
            AiProviderConfigurationValidator.validate(configuration.copy(allowInsecureHttp = true)).isValid
        )
    }

    @Test
    fun rejectsMalformedOrCredentialBearingUrl() {
        val invalidUrls = listOf(
            "not a url",
            "https:///missing-host",
            "https://user:password@example.com/v1",
            "https://example.com/v1?token=private",
            " https://example.com/v1",
            "https://example.com:70000/v1"
        )

        invalidUrls.forEach { url ->
            assertFalse(url, AiProviderConfigurationValidator.isBaseUrlAllowed(url))
        }
    }

    @Test
    fun validatesSupportedGenerationParameters() {
        val invalid = DefaultAiProviderConfiguration.value.copy(
            parameters = AiGenerationParameters(temperature = 2.1f, topP = -0.1f, maxOutputTokens = 0)
        )

        assertFalse(AiProviderConfigurationValidator.validate(invalid).isValid)
        assertTrue(
            AiProviderConfigurationValidator.validate(
                invalid.copy(parameters = AiGenerationParameters(temperature = 0f, topP = 1f, maxOutputTokens = 1))
            ).isValid
        )
    }
}
