package dev.odmd.platform.springcdk.domain.cryptography

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.springframework.stereotype.Component

data class SerializedCiphertext(val ciphertext: String, val keyVersion: String)

@Component
class TransitSerializer : StdSerializer<Any>(Any::class.java) {
    override fun serialize(value: Any, gen: JsonGenerator, provider: SerializerProvider) {
        val objectMapper = gen.codec as ObjectMapper
        val plaintext = objectMapper.writeValueAsString(value).encodeToByteArray()
        val ciphertext = TransitConfiguration.cryptographyService.encrypt(plaintext, TransitConfiguration.transitContextService.getContext())
        gen.writeObject(ciphertext)
    }
}

