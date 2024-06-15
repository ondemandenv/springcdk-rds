package dev.odmd.platform.springcdk.domain.cryptography

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.introspect.Annotated
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector
import com.vladmihalcea.hibernate.type.util.ObjectMapperSupplier
import org.springframework.stereotype.Component

/**
 * Pick up the [JsonEncrypted] annotation and use the correct serializer and deserializer.
 */
class TransitSerializerAnnotationInterceptor : NopAnnotationIntrospector() {
    override fun findSerializer(am: Annotated): Any? {
        if (am.hasAnnotation(dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted::class.java)) {
            return TransitSerializer::class.java
        }

        return super.findSerializer(am)
    }

    override fun findDeserializer(am: Annotated): Any? {
        if (am.hasAnnotation(dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted::class.java)) {
            return ContextualTransitDeserializer::class.java
        }

        return super.findDeserializer(am)
    }
}

@Component("encryptionHibernateObjectMapper")
class EncryptionObjectMapperSupplier : ObjectMapperSupplier {
    override fun get(): ObjectMapper {
        val mapper = ObjectMapper().findAndRegisterModules()
        val defaultInterceptor = mapper.serializationConfig.annotationIntrospector
        val pair = AnnotationIntrospectorPair(TransitSerializerAnnotationInterceptor(), defaultInterceptor)
        mapper.setAnnotationIntrospector(pair)
        return mapper
    }
}
