package dev.odmd.platform.springcdk.domain.cryptography

import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope

/**
 * We require the use of derived keys in Vault in order to enhance security. Derived keys add a requirement to use an
 * additional piece of information to encrypt or decrypt specific pieces of data, which in Vault language is called a
 * context.
 *
 * To use a derived key, the context - a byte array - is required. Ideally, this is something
 * unique to the entity on behalf of whom we are doing the encryption.
 *
 * In the context of an HTTP request, this probably comes from a header. For a database operation,
 * you may be able to pull it from the row containing the encrypted data.
 */
interface TransitContextService {
    fun getContext(): ByteArray
}

/**
 * Allows specifying a transit context that's scoped to the current request.
 *
 * In situations where the transit context cannot be set automatically, components can add this
 * class as a dependency (via [Autowire]) and manually set [scopedContext] before interacting with
 * the database.
 */
@Service
@RequestScope
class ScopedTransitContextService {
    var scopedContext: ByteArray? = null
}
