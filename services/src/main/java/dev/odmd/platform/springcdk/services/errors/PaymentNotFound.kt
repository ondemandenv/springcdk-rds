package dev.odmd.platform.springcdk.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
data class PaymentNotFound(
    val paymentId: String
) : PaymentServiceError, Exception("Payment $paymentId not found.")
