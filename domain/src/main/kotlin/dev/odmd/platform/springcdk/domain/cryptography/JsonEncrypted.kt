package dev.odmd.platform.springcdk.domain.cryptography

/**
 * Annotation used by [TransitSerializerAnnotationInterceptor] to encrypt and decrypt JSON fields as they're serialized
 * and deserialized
 *
 * We currently only use [TransitSerializerAnnotationInterceptor] on the hibernate-types
 * [ObjectMapper][com.fasterxml.jackson.databind.ObjectMapper], so it only affects fields that are being serialized and deserialized
 * to and from the database. You could implement the same thing on API interfaces by adding the annotation interceptor
 * to the `ObjectMapper` used by the API, and could avoid some of the hacks we've used in order to make this work with
 * Hibernate.
 *
 * Please note that, as implemented, this introduces **substantial performance overhead** (~10ms) when
 * saving an entity. The Vault Transit engine is Encryption as a Service, so using this introduces network round-trips
 * during JSON serialization and deserialization.
 *
 * Example usage:
 *
 * `data class ClassWithPiiField(val plaintext: String, &#64;field:JsonEncrypted val pii: String)`
 *
 * That will be serialized to:
 *
 * `{"plaintext": "", "pii": {"ciphertext": "...", "keyVersion": ".."}}`
 *
 * You can also annotate classes directly:
 *
 * `data class ClassWithPiiClass(&#64;field:JsonEncrypted val piiClass: PiiClass)`
 *
 * That will be serialized to:
 *
 * `{"piiClass": {"ciphertext": "...", "keyVersion": ".."}}`
 */
@Target(AnnotationTarget.FIELD)
annotation class JsonEncrypted
