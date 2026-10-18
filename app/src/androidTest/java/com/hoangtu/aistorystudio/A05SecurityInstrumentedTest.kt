package com.hoangtu.aistorystudio

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hoangtu.aistorystudio.core.security.AndroidKeyStoreSecureKeyStore
import com.hoangtu.aistorystudio.core.security.EncryptedApiKey
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreError
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreResult
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class A05SecurityInstrumentedTest {
    private val secureKeyStore = AndroidKeyStoreSecureKeyStore()

    @Test
    fun keystoreRoundTripUsesAuthenticatedCiphertextAndSurvivesNewInstance() {
        val ownerId = "instrumented-provider-${System.nanoTime()}"
        val secret = "fixture-secret-for-gcm-test-only-123".toByteArray(StandardCharsets.UTF_8)
        val first = successfulEncryption(ownerId, secret)
        val second = successfulEncryption(ownerId, secret)

        assertFalse(String(first.ciphertext, StandardCharsets.ISO_8859_1).contains(String(secret)))
        assertFalse(first.initializationVector.contentEquals(second.initializationVector))

        val reopenedKeyStore = AndroidKeyStoreSecureKeyStore()
        val decrypted = reopenedKeyStore.decrypt(ownerId, first)
        assertTrue(decrypted is SecureKeyStoreResult.Success)
        val actual = (decrypted as SecureKeyStoreResult.Success).value
        assertArrayEquals(secret, actual)
        actual.fill(0)

        val otherOwnerResult = secureKeyStore.decrypt("different-owner", first)
        assertEqualsError(SecureKeyStoreError.CIPHERTEXT_CORRUPTED, otherOwnerResult)

        val tamperedCiphertext = first.ciphertext.copyOf().also {
            it[it.lastIndex] = (it.last().toInt() xor 1).toByte()
        }
        val tamperedResult = secureKeyStore.decrypt(ownerId, first.copy(ciphertext = tamperedCiphertext))
        assertEqualsError(SecureKeyStoreError.CIPHERTEXT_CORRUPTED, tamperedResult)

        assertTrue(secureKeyStore.deleteKey(first.keyAlias) is SecureKeyStoreResult.Success)
        assertEqualsError(SecureKeyStoreError.KEY_NOT_FOUND, secureKeyStore.decrypt(ownerId, first))
        secureKeyStore.deleteKey(second.keyAlias)
    }

    @Test
    fun backupRulesExcludeProviderDataStoreFromCloudAndDeviceTransfer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val excludedPaths = readExclusions(context, R.xml.data_extraction_rules)

        assertTrue(excludedPaths.contains("cloud-backup|file/datastore/ai_provider_settings.preferences_pb"))
        assertTrue(excludedPaths.contains("device-transfer|file/datastore/ai_provider_settings.preferences_pb"))

        val legacyExcludedPaths = readExclusions(context, R.xml.backup_rules)
        assertTrue(legacyExcludedPaths.contains("full-backup-content|file/datastore/ai_provider_settings.preferences_pb"))
    }

    private fun successfulEncryption(ownerId: String, secret: ByteArray): EncryptedApiKey {
        val result = secureKeyStore.encrypt(ownerId, secret)
        assertTrue(result is SecureKeyStoreResult.Success)
        return (result as SecureKeyStoreResult.Success).value
    }

    private fun assertEqualsError(
        expected: SecureKeyStoreError,
        result: SecureKeyStoreResult<*>
    ) {
        assertTrue(result is SecureKeyStoreResult.Failure)
        assertTrue((result as SecureKeyStoreResult.Failure).error == expected)
    }

    private fun readExclusions(context: Context, resourceId: Int): Set<String> {
        val parser = context.resources.getXml(resourceId)
        val results = mutableSetOf<String>()
        var currentSection = ""
        try {
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "cloud-backup", "device-transfer", "full-backup-content" -> currentSection = parser.name
                        "exclude" -> {
                            val domain = parser.getAttributeValue(null, "domain")
                            val path = parser.getAttributeValue(null, "path")
                            results += "$currentSection|$domain/$path"
                        }
                    }
                }
                parser.next()
            }
        } finally {
            parser.close()
        }
        return results
    }
}
