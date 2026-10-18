package com.hoangtu.aistorystudio.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidKeyStoreSecureKeyStore : SecureKeyStore {
    override fun encrypt(
        ownerId: String,
        plaintext: ByteArray
    ): SecureKeyStoreResult<EncryptedApiKey> {
        if (ownerId.isBlank() || plaintext.isEmpty()) {
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.INVALID_INPUT)
        }
        val alias = KEY_ALIAS_PREFIX + UUID.randomUUID().toString()
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_SIZE_BITS)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            val key = keyGenerator.generateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(ownerId.toByteArray(Charsets.UTF_8))
            val plaintextCopy = plaintext.copyOf()
            val ciphertext = try {
                cipher.doFinal(plaintextCopy)
            } finally {
                plaintextCopy.fill(0)
            }
            val iv = cipher.iv
            if (iv.size != GCM_IV_LENGTH_BYTES) {
                deleteKey(alias)
                SecureKeyStoreResult.Failure(SecureKeyStoreError.KEYSTORE_UNAVAILABLE)
            } else {
                SecureKeyStoreResult.Success(
                    EncryptedApiKey(
                        keyAlias = alias,
                        initializationVector = iv,
                        ciphertext = ciphertext
                    )
                )
            }
        } catch (_: Exception) {
            deleteKey(alias)
            SecureKeyStoreResult.Failure(SecureKeyStoreError.KEYSTORE_UNAVAILABLE)
        }
    }

    override fun decrypt(
        ownerId: String,
        encryptedApiKey: EncryptedApiKey
    ): SecureKeyStoreResult<ByteArray> {
        if (ownerId.isBlank() || encryptedApiKey.initializationVector.size != GCM_IV_LENGTH_BYTES ||
            encryptedApiKey.ciphertext.size < GCM_TAG_LENGTH_BYTES
        ) {
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        }
        return try {
            val keyStore = loadKeyStore()
            val key = keyStore.getKey(encryptedApiKey.keyAlias, null) as? SecretKey
                ?: return SecureKeyStoreResult.Failure(SecureKeyStoreError.KEY_NOT_FOUND)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                key,
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, encryptedApiKey.initializationVector)
            )
            cipher.updateAAD(ownerId.toByteArray(Charsets.UTF_8))
            SecureKeyStoreResult.Success(cipher.doFinal(encryptedApiKey.ciphertext))
        } catch (_: KeyPermanentlyInvalidatedException) {
            SecureKeyStoreResult.Failure(SecureKeyStoreError.KEY_INVALIDATED)
        } catch (_: UnrecoverableKeyException) {
            SecureKeyStoreResult.Failure(SecureKeyStoreError.KEY_INVALIDATED)
        } catch (_: AEADBadTagException) {
            SecureKeyStoreResult.Failure(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        } catch (_: Exception) {
            SecureKeyStoreResult.Failure(SecureKeyStoreError.KEYSTORE_UNAVAILABLE)
        }
    }

    override fun deleteKey(keyAlias: String): SecureKeyStoreResult<Unit> {
        if (!keyAlias.startsWith(KEY_ALIAS_PREFIX)) {
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.INVALID_INPUT)
        }
        return try {
            loadKeyStore().deleteEntry(keyAlias)
            SecureKeyStoreResult.Success(Unit)
        } catch (_: Exception) {
            SecureKeyStoreResult.Failure(SecureKeyStoreError.KEYSTORE_UNAVAILABLE)
        }
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS_PREFIX = "ai_story_studio_api_key_"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
        const val GCM_TAG_LENGTH_BYTES = GCM_TAG_LENGTH_BITS / 8
        const val GCM_IV_LENGTH_BYTES = 12
    }
}
