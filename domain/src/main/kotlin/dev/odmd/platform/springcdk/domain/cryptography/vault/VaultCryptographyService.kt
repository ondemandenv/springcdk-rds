package dev.odmd.platform.springcdk.domain.cryptography.vault

import dev.odmd.platform.springcdk.domain.cryptography.CryptographyService
import dev.odmd.platform.springcdk.domain.cryptography.SerializedCiphertext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "app.vault", value = ["enabled"], havingValue = "true")
class VaultCryptographyService(
//    val vaultOperations: VaultOperations,
    val appVaultConfiguration: AppVaultConfiguration
) : dev.odmd.platform.springcdk.domain.cryptography.CryptographyService {
    override fun encrypt(plaintext: ByteArray, context: ByteArray): SerializedCiphertext {
        val ciphertextString = ""
        return SerializedCiphertext(
            ciphertextString,
            ciphertextString.split(':')[1] // vault serializes strings in the format vault:vNNN:ciphertext
        )
    }

    override fun decrypt(ciphertext: SerializedCiphertext, context: ByteArray): ByteArray {
        TODO("Not yet implemented")
    }

}
