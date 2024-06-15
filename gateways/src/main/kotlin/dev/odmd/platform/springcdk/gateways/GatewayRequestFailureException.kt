package dev.odmd.platform.springcdk.gateways

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Generic exception for gateway requests that receive an unsuccessful response,
 * such as a declined transaction or invalid credit card.
 *
 * This is only intended for errors reported by the gateway. Any other kinds of
 * errors (e.g. connection timeout) should be handled separately.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
open class GatewayRequestFailureException(
    val gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    message: String,
    cause: Throwable? = null
) : RuntimeException("Gateway request $gatewayRequestId failed: $message", cause)
