package com.hoangtu.aistorystudio

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangtu.aistorystudio.core.ai.settings.ApiKeyReadResult
import com.hoangtu.aistorystudio.core.ai.settings.DefaultAiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.DeleteApiKeyResult
import com.hoangtu.aistorystudio.core.ai.settings.SaveProviderResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class A05CredentialRestartInstrumentedTest {
    @Test
    fun savedApiKeyCanBeReadAfterAProcessRestart() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as StudioApplication
        val repository = application.aiProviderSettingsRepository
        val providerId = DefaultAiProviderConfiguration.PROVIDER_ID
        val fixture = "a05-process-restart-test-fixture".toByteArray(Charsets.UTF_8)
        val phase = InstrumentationRegistry.getArguments().getString("phase")

        repository.ensureDefaultProvider()
        if (phase != "verify") {
            val saved = repository.saveProvider(DefaultAiProviderConfiguration.value, fixture)
            assertTrue(saved is SaveProviderResult.Saved)
        }

        if (phase != "seed") {
            val read = repository.readApiKey(providerId)
            assertTrue("API key cannot be read from DataStore/Android Keystore", read is ApiKeyReadResult.Available)
            val actual = (read as ApiKeyReadResult.Available).value
            assertArrayEquals(fixture, actual)
            actual.fill(0)
            if (phase == "verify" || phase == null) {
                assertTrue(repository.deleteApiKey(providerId) is DeleteApiKeyResult.Deleted)
            }
        }
    }
}
