package dev.odmd.platform.springcdk.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidRefundState(
    val requestId: String,
    val msg: String
) : PaymentServiceError, Exception("$requestId: Invalid input for refund: $msg.")
