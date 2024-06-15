package dev.odmd.platform.springcdk.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidRefundInput(
    val requestId: String,
    val msg: String
) : PaymentServiceError, Exception("$requestId: Invalid input for refund: $msg.")

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidRefundInputInsufficient(
    val requestId: String,
    val msg: String
) : PaymentServiceError, Exception("$requestId: Invalid input for refund: $msg.")

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidRefundInputNotFound(
    val requestId: String,
    val msg: String
) : PaymentServiceError, Exception("$requestId: Invalid input for refund: $msg.")

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidRefundInputItemNotfound(
    val requestId: String,
    val msg: String
) : PaymentServiceError, Exception("$requestId: Invalid input for refund: $msg.")
