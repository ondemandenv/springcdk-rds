package dev.odmd.platform.springcdk.domain.cryptography

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

@Service
// This instantiates this service only when no other [CryptographyService] is available
@ConditionalOnMissingBean(value = [dev.odmd.platform.springcdk.domain.cryptography.CryptographyService::class], ignored = [dev.odmd.platform.springcdk.domain.cryptography.NoopCryptographyService::class])
class NoopCryptographyService : dev.odmd.platform.springcdk.domain.cryptography.CryptographyService {
    override fun encrypt(plaintext: ByteArray, context: ByteArray): dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext {
        return dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext(
            plaintext.decodeToString(),
            ""
        )
    }

    override fun decrypt(ciphertext: dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext, context: ByteArray): ByteArray {
        return ciphertext.ciphertext.encodeToByteArray()
    }
}
