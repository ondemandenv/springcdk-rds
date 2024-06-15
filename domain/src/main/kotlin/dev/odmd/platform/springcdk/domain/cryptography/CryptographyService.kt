package dev.odmd.platform.springcdk.domain.cryptography

interface CryptographyService {
    fun encrypt(plaintext: ByteArray, context: ByteArray): dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext

    fun decrypt(ciphertext: dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext, context: ByteArray): ByteArray
}
