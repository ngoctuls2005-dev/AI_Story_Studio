package com.hoangtu.aistorystudio.core.ai.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreError
import java.io.File
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreAiProviderSettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultProviderIsSeededWithRequestedValuesAndNoApiKey() = runTest {
        val harness = createHarness(File(temporaryFolder.root, "settings.preferences_pb"), testScheduler)
        try {
            harness.repository.ensureDefaultProvider()

            val profile = harness.repository.observeProviders().first().single()
            assertEquals("Experiential Labs", profile.configuration.displayName)
            assertEquals("https://api.experientiallabs.ai/v1", profile.configuration.baseUrl)
            assertEquals("deepseek-v4.1-flash-uncensored", profile.configuration.modelId)
            assertEquals(CredentialStatus.MISSING, profile.credentialStatus)
            assertEquals(LocalConfigurationStatus.MISSING_API_KEY, profile.localStatus)
        } finally {
            harness.close()
        }
    }

    @Test
    fun savesEncryptedKeyWithoutPlaintextAndReadsItBack() = runTest {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        val harness = createHarness(file, testScheduler)
        val apiKey = "sk-test-秘密-đã dấu-9281"
        try {
            harness.repository.ensureDefaultProvider()
            val result = harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                apiKey.toByteArray(StandardCharsets.UTF_8)
            )

            assertTrue(result is SaveProviderResult.Saved)
            val profile = harness.repository.observeProviders().first().single()
            assertEquals(CredentialStatus.AVAILABLE, profile.credentialStatus)
            val readResult = harness.repository.readApiKey(DefaultAiProviderConfiguration.PROVIDER_ID)
            assertTrue(readResult is ApiKeyReadResult.Available)
            val secret = (readResult as ApiKeyReadResult.Available).value
            assertArrayEquals(apiKey.toByteArray(StandardCharsets.UTF_8), secret)
            secret.fill(0)

            val serializedPreferences = String(file.readBytes(), StandardCharsets.ISO_8859_1)
            assertFalse(serializedPreferences.contains(apiKey))
        } finally {
            harness.close()
        }
    }

    @Test
    fun providersAndKeyRotationsRemainIsolated() = runTest {
        val harness = createHarness(File(temporaryFolder.root, "settings.preferences_pb"), testScheduler)
        val secondProvider = DefaultAiProviderConfiguration.value.copy(
            providerId = "provider-two",
            displayName = "Provider Two",
            baseUrl = "https://second.example/v1",
            modelId = "second-model"
        )
        try {
            harness.repository.ensureDefaultProvider()
            harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "first-key-old".toByteArray()
            )
            harness.repository.saveProvider(secondProvider, "second-key".toByteArray())
            assertTrue(harness.repository.selectProvider(secondProvider.providerId))
            val firstAlias = harness.dataStore.data.first()[stringKeyForTest(
                DefaultAiProviderConfiguration.PROVIDER_ID,
                "key_alias"
            )]
            harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "first-key-new".toByteArray()
            )

            assertEquals("first-key-new", readSecret(harness.repository, DefaultAiProviderConfiguration.PROVIDER_ID))
            assertEquals("second-key", readSecret(harness.repository, secondProvider.providerId))
            val profiles = harness.repository.observeProviders().first()
            assertFalse(profiles.single { it.configuration.providerId == DefaultAiProviderConfiguration.PROVIDER_ID }.isActive)
            assertTrue(profiles.single { it.configuration.providerId == secondProvider.providerId }.isActive)
            assertNotNull(firstAlias)
            assertTrue(harness.secureKeyStore.deletedAliases.contains(firstAlias))
        } finally {
            harness.close()
        }
    }

    @Test
    fun deleteRemovesStoredCiphertextAndMakesPreviousKeyUnavailable() = runTest {
        val harness = createHarness(File(temporaryFolder.root, "settings.preferences_pb"), testScheduler)
        try {
            harness.repository.ensureDefaultProvider()
            harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "delete-me".toByteArray()
            )

            assertEquals(DeleteApiKeyResult.Deleted, harness.repository.deleteApiKey(
                DefaultAiProviderConfiguration.PROVIDER_ID
            ))
            assertTrue(harness.repository.readApiKey(DefaultAiProviderConfiguration.PROVIDER_ID) is ApiKeyReadResult.Missing)
            assertEquals(CredentialStatus.MISSING, harness.repository.observeProviders().first().single().credentialStatus)
        } finally {
            harness.close()
        }
    }

    @Test
    fun corruptedCiphertextRequiresReentryWithoutRemovingProviderConfiguration() = runTest {
        val harness = createHarness(File(temporaryFolder.root, "settings.preferences_pb"), testScheduler)
        try {
            harness.repository.ensureDefaultProvider()
            harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "tamper-check".toByteArray()
            )
            harness.dataStore.edit { preferences ->
                val key = byteArrayPreferencesKey("provider.${DefaultAiProviderConfiguration.PROVIDER_ID}.key_ciphertext")
                preferences[key] = preferences[key]!!.copyOf().also { bytes ->
                    bytes[0] = (bytes[0].toInt() xor 1).toByte()
                }
            }

            val profile = harness.repository.observeProviders().first().single()
            assertEquals("Experiential Labs", profile.configuration.displayName)
            assertEquals(CredentialStatus.REENTRY_REQUIRED, profile.credentialStatus)
            val result = harness.repository.readApiKey(DefaultAiProviderConfiguration.PROVIDER_ID)
            assertTrue(result is ApiKeyReadResult.NeedsReentry)
            assertEquals(
                SecureKeyStoreError.CIPHERTEXT_CORRUPTED,
                (result as ApiKeyReadResult.NeedsReentry).reason
            )
        } finally {
            harness.close()
        }
    }

    @Test
    fun keystoreFailureIsSanitizedAndCiphertextIsRetainedForReentry() = runTest {
        val harness = createHarness(File(temporaryFolder.root, "settings.preferences_pb"), testScheduler)
        try {
            harness.repository.ensureDefaultProvider()
            harness.secureKeyStore.nextEncryptionError = SecureKeyStoreError.KEYSTORE_UNAVAILABLE
            val failedSave = harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "not-in-error-message".toByteArray()
            )
            assertEquals(
                SaveProviderResult.EncryptionFailed(SecureKeyStoreError.KEYSTORE_UNAVAILABLE),
                failedSave
            )
            assertFalse(failedSave.toString().contains("not-in-error-message"))

            harness.repository.saveProvider(
                DefaultAiProviderConfiguration.value,
                "persisted-test-key".toByteArray()
            )
            val alias = harness.dataStore.data.first()[stringKeyForTest(
                DefaultAiProviderConfiguration.PROVIDER_ID,
                "key_alias"
            )]
            assertNotNull(alias)
            harness.secureKeyStore.decryptErrorsByAlias[alias!!] = SecureKeyStoreError.KEY_INVALIDATED

            val result = harness.repository.readApiKey(DefaultAiProviderConfiguration.PROVIDER_ID)
            assertEquals(SecureKeyStoreError.KEY_INVALIDATED, (result as ApiKeyReadResult.NeedsReentry).reason)
            assertEquals(CredentialStatus.REENTRY_REQUIRED, harness.repository.observeProviders().first().single().credentialStatus)
            assertNotNull(harness.dataStore.data.first()[stringKeyForTest(
                DefaultAiProviderConfiguration.PROVIDER_ID,
                "key_alias"
            )])
        } finally {
            harness.close()
        }
    }

    @Test
    fun configurationSurvivesDataStoreReopenWithKeyReadable() = runTest {
        val file = File(temporaryFolder.root, "settings.preferences_pb")
        val secureKeyStore = FakeSecureKeyStore()
        val original = createHarness(file, testScheduler, secureKeyStore)
        original.repository.ensureDefaultProvider()
        original.repository.saveProvider(
            DefaultAiProviderConfiguration.value,
            "reopen-test-key".toByteArray()
        )
        original.close()

        val reopened = createHarness(file, testScheduler, secureKeyStore)
        try {
            assertEquals("reopen-test-key", readSecret(reopened.repository, DefaultAiProviderConfiguration.PROVIDER_ID))
            assertEquals(CredentialStatus.AVAILABLE, reopened.repository.observeProviders().first().single().credentialStatus)
        } finally {
            reopened.close()
        }
    }

    private suspend fun readSecret(
        repository: AiProviderSettingsRepository,
        providerId: String
    ): String {
        val result = repository.readApiKey(providerId) as ApiKeyReadResult.Available
        return result.value.toString(StandardCharsets.UTF_8).also { result.value.fill(0) }
    }

    private fun stringKeyForTest(providerId: String, field: String) =
        androidx.datastore.preferences.core.stringPreferencesKey("provider.$providerId.$field")

    private fun createHarness(
        file: File,
        scheduler: TestCoroutineScheduler,
        secureKeyStore: FakeSecureKeyStore = FakeSecureKeyStore()
    ): RepositoryHarness {
        val dispatcher = UnconfinedTestDispatcher(scheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val dataStore = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { file }
        )
        return RepositoryHarness(
            dataStore = dataStore,
            repository = DataStoreAiProviderSettingsRepository(dataStore, secureKeyStore, dispatcher),
            secureKeyStore = secureKeyStore,
            scope = scope
        )
    }

    private data class RepositoryHarness(
        val dataStore: DataStore<Preferences>,
        val repository: DataStoreAiProviderSettingsRepository,
        val secureKeyStore: FakeSecureKeyStore,
        val scope: CoroutineScope
    ) {
        fun close() = scope.cancel()
    }
}
