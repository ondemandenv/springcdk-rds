package dev.odmd.platform.springcdk.ui

import dev.odmd.platform.springcdk.common.RequestHeaders.ODMD_ENTITY_ID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.context.annotation.RequestScope
import org.springframework.web.server.ResponseStatusException

/**
 * All requests require the `odmd-eee-id` header to be present. This is the identifier of the entity that owns the
 * payment profile.
 *
 * We parse that header out and use it as the context for our encryption engine, so calls with different
 * `odmd-eee-id` that reference the same profile will fail.
 */
@Service
@RequestScope
class DelegatingScopedTransitContextService(private val scopedTransitContextService: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService) :
    dev.odmd.platform.springcdk.domain.cryptography.TransitContextService {
    override fun getContext(): ByteArray {
        return scopedTransitContextService.scopedContext ?: throw ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "$ODMD_ENTITY_ID required"
        )
    }
}
