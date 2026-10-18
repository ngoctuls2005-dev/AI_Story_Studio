package com.hoangtu.aistorystudio.core.security

data class EncryptedApiKey(
    val keyAlias: String,
    val initializationVector: ByteArray,
    val ciphertext: ByteArray
)

enum class SecureKeyStoreError {
    KEYSTORE_UNAVAILABLE,
    KEY_INVALIDATED,
    KEY_NOT_FOUND,
    CIPHERTEXT_CORRUPTED,
    INVALID_INPUT
}

sealed interface SecureKeyStoreResult<out T> {
    data class Success<T>(val value: T) : SecureKeyStoreResult<T>
    data class Failure(val error: SecureKeyStoreError) : SecureKeyStoreResult<Nothing>
}

interface SecureKeyStore {
    fun encrypt(ownerId: String, plaintext: ByteArray): SecureKeyStoreResult<EncryptedApiKey>
    fun decrypt(ownerId: String, encryptedApiKey: EncryptedApiKey): SecureKeyStoreResult<ByteArray>
    fun deleteKey(keyAlias: String): SecureKeyStoreResult<Unit>
}
