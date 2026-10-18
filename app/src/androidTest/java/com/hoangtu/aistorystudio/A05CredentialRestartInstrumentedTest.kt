package com.hoangtu.aistorystudio

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangtu.aistorystudio.core.datastore.aiProviderDataStore
import com.hoangtu.aistorystudio.core.ai.settings.ApiKeyReadResult
import com.hoangtu.aistorystudio.core.ai.settings.DataStoreAiProviderSettingsRepository
import com.hoangtu.aistorystudio.core.ai.settings.DefaultAiProviderConfiguration
import com.hoangtu.aistorystudio.core.ai.settings.DeleteApiKeyResult
import com.hoangtu.aistorystudio.core.ai.settings.SaveProviderResult
import com.hoangtu.aistorystudio.core.security.AndroidKeyStoreSecureKeyStore
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreResult
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
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
        val testDataStoreFile = File(context.cacheDir, "a06-provider-settings-test.preferences_pb")
        val storeJob = SupervisorJob()
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(storeJob + Dispatchers.IO),
            produceFile = { testDataStoreFile }
        )
        val repository = DataStoreAiProviderSettingsRepository(
            dataStore = testDataStore,
            secureKeyStore = AndroidKeyStoreSecureKeyStore()
        )
        val providerId = InstrumentationRegistry.getArguments()
            .getString("providerId")
            ?: "a06-test-process-restart"
        val testConfiguration = DefaultAiProviderConfiguration.value.copy(
            providerId = providerId,
            displayName = "Provider test khoi dong lai"
        )
        val fixture = "a05-process-restart-test-fixture".toByteArray(Charsets.UTF_8)
        val phase = InstrumentationRegistry.getArguments().getString("phase")

        try {
            repository.ensureDefaultProvider()
            if (phase != "verify" && phase != "cleanup") {
                val saved = repository.saveProvider(testConfiguration, fixture)
                assertTrue(saved is SaveProviderResult.Saved)
            }

            if (phase == "cleanup") {
                assertTrue(repository.deleteApiKey(providerId) is DeleteApiKeyResult.Deleted)
                cleanupLegacyApplicationFixture(context, providerId)
            } else if (phase != "seed") {
                val read = repository.readApiKey(providerId)
                assertTrue("API key cannot be read from DataStore/Android Keystore", read is ApiKeyReadResult.Available)
                val actual = (read as ApiKeyReadResult.Available).value
                assertArrayEquals(fixture, actual)
                actual.fill(0)
                if (phase == "verify" || phase == null) {
                    assertTrue(repository.deleteApiKey(providerId) is DeleteApiKeyResult.Deleted)
                }
            }
        } finally {
            fixture.fill(0)
            if (phase != "seed") {
                assertTrue(repository.deleteApiKey(providerId) is DeleteApiKeyResult.Deleted)
            }
            storeJob.cancelAndJoin()
            if (phase != "seed" && testDataStoreFile.exists()) {
                assertTrue(testDataStoreFile.delete())
            }
            if (phase != "seed") assertTrue(!testDataStoreFile.exists())
        }
    }

    private suspend fun cleanupLegacyApplicationFixture(context: android.content.Context, providerId: String) {
        var keyAlias: String? = null
        val appDataStore = context.applicationContext.aiProviderDataStore
        appDataStore.edit { preferences ->
            val displayNameKey = stringPreferencesKey("provider.$providerId.display_name")
            if (preferences[displayNameKey] == "Provider test khoi dong lai") {
                keyAlias = preferences[stringPreferencesKey("provider.$providerId.key_alias")]
                preferences[stringSetPreferencesKey("provider_ids")] =
                    preferences[stringSetPreferencesKey("provider_ids")].orEmpty() - providerId
                if (preferences[stringPreferencesKey("active_provider_id")] == providerId) {
                    preferences[stringPreferencesKey("active_provider_id")] =
                        DefaultAiProviderConfiguration.PROVIDER_ID
                }
                preferences.remove(displayNameKey)
                preferences.remove(stringPreferencesKey("provider.$providerId.base_url"))
                preferences.remove(stringPreferencesKey("provider.$providerId.model_id"))
                preferences.remove(booleanPreferencesKey("provider.$providerId.allow_insecure_http"))
                preferences.remove(floatPreferencesKey("provider.$providerId.temperature"))
                preferences.remove(floatPreferencesKey("provider.$providerId.top_p"))
                preferences.remove(intPreferencesKey("provider.$providerId.max_output_tokens"))
                preferences.remove(stringPreferencesKey("provider.$providerId.key_alias"))
                preferences.remove(byteArrayPreferencesKey("provider.$providerId.key_iv"))
                preferences.remove(byteArrayPreferencesKey("provider.$providerId.key_ciphertext"))
            }
        }
        keyAlias?.let { alias ->
            assertTrue(AndroidKeyStoreSecureKeyStore().deleteKey(alias) is SecureKeyStoreResult.Success)
        }
        val remainingPreferences = appDataStore.data.first()
        assertTrue(providerId !in remainingPreferences[stringSetPreferencesKey("provider_ids")].orEmpty())
        assertTrue(!remainingPreferences.contains(stringPreferencesKey("provider.$providerId.key_alias")))
    }
}
