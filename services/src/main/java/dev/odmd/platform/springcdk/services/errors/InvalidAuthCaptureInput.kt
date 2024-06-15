package dev.odmd.platform.springcdk.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidAuthCaptureInput(
    val requestId: String,
    msg: String,
    cause: Throwable? = null
) : PaymentServiceError, Exception("$requestId: Invalid input for auth: $msg.", cause)
