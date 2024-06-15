package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class ProxiedRequestDeserializationException(
    cause: Throwable
) : RuntimeException("Unable to deserialize request for proxy", cause)

@ResponseStatus(HttpStatus.BAD_REQUEST)
open class ProxiedRequestMissingParametersException(
    fieldName: String,
    cause: Throwable? = null
) : RuntimeException("Missing field $fieldName on proxied request", cause)

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
open class ProxiedRequestUnsupportedOperationException(
    operationName: String,
    cause: Throwable? = null
) : RuntimeException("Unsupported operation $operationName on proxied request", cause)

