package dev.odmd.platform.springcdk.domain.cryptography

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.springframework.stereotype.Component

@Component
class ContextualTransitDeserializer : StdDeserializer<Any>(Any::class.java), ContextualDeserializer {
    /**
     * We split out the "real" deserialization into [TransitDeserializer] - having the deserialization happen in
     * [ContextualTransitDeserializer] created a confusing dual-use class situation, where the Spring-constructed and
     * Hibernate-constructed instances would never be used for deserialization, and custom instances with the
     * appropriate type never meaningfully implemented [ContextualDeserializer]
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any {
        throw NotImplementedError("Use TypedTransitDeserializer")
    }

    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty): JsonDeserializer<*> {
        return TransitDeserializer(property.type)
    }
}

class TransitDeserializer(
    private val type: JavaType
) : StdDeserializer<Any>(Any::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Any {
        val serializedCiphertext = p.readValueAs(SerializedCiphertext::class.java)
        val deserializedString = TransitConfiguration.cryptographyService.decrypt(serializedCiphertext, TransitConfiguration.transitContextService.getContext())
        val objectMapper = p.codec as ObjectMapper

        return objectMapper.readValue(deserializedString, type)
    }
}
