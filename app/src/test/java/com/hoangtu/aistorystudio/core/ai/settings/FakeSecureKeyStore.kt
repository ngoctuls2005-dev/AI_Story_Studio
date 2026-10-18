package com.hoangtu.aistorystudio.core.ai.settings

import com.hoangtu.aistorystudio.core.security.EncryptedApiKey
import com.hoangtu.aistorystudio.core.security.SecureKeyStore
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreError
import com.hoangtu.aistorystudio.core.security.SecureKeyStoreResult
import java.security.MessageDigest

internal class FakeSecureKeyStore : SecureKeyStore {
    private val ownersByAlias = mutableMapOf<String, String>()
    private var nextAlias = 0
    var nextEncryptionError: SecureKeyStoreError? = null
    val decryptErrorsByAlias = mutableMapOf<String, SecureKeyStoreError>()
    val deletedAliases = mutableSetOf<String>()

    override fun encrypt(ownerId: String, plaintext: ByteArray): SecureKeyStoreResult<EncryptedApiKey> {
        nextEncryptionError?.let {
            nextEncryptionError = null
            return SecureKeyStoreResult.Failure(it)
        }
        val alias = "test_ai_key_${++nextAlias}"
        ownersByAlias[alias] = ownerId
        val iv = ByteArray(12) { nextAlias.toByte() }
        val checksum = checksum(ownerId, plaintext)
        val ciphertext = ByteArray(plaintext.size + checksum.size)
        plaintext.forEachIndexed { index, byte -> ciphertext[index] = (byte.toInt() xor MASK).toByte() }
        checksum.copyInto(ciphertext, plaintext.size)
        return SecureKeyStoreResult.Success(EncryptedApiKey(alias, iv, ciphertext))
    }

    override fun decrypt(
        ownerId: String,
        encryptedApiKey: EncryptedApiKey
    ): SecureKeyStoreResult<ByteArray> {
        decryptErrorsByAlias[encryptedApiKey.keyAlias]?.let {
            return SecureKeyStoreResult.Failure(it)
        }
        if (ownersByAlias[encryptedApiKey.keyAlias] != ownerId) {
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        }
        if (encryptedApiKey.ciphertext.size < CHECKSUM_SIZE) {
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        }
        val plaintextSize = encryptedApiKey.ciphertext.size - CHECKSUM_SIZE
        val plaintext = ByteArray(plaintextSize) { index ->
            (encryptedApiKey.ciphertext[index].toInt() xor MASK).toByte()
        }
        val storedChecksum = encryptedApiKey.ciphertext.copyOfRange(plaintextSize, encryptedApiKey.ciphertext.size)
        if (!MessageDigest.isEqual(storedChecksum, checksum(ownerId, plaintext))) {
            plaintext.fill(0)
            return SecureKeyStoreResult.Failure(SecureKeyStoreError.CIPHERTEXT_CORRUPTED)
        }
        return SecureKeyStoreResult.Success(plaintext)
    }

    override fun deleteKey(keyAlias: String): SecureKeyStoreResult<Unit> {
        ownersByAlias.remove(keyAlias)
        deletedAliases += keyAlias
        return SecureKeyStoreResult.Success(Unit)
    }

    private fun checksum(ownerId: String, plaintext: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(ownerId.toByteArray(Charsets.UTF_8))
        return digest.digest(plaintext)
    }

    private companion object {
        const val MASK = 0x5A
        const val CHECKSUM_SIZE = 32
    }
}
