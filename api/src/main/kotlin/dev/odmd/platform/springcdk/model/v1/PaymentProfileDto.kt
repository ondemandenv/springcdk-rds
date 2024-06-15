package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode

data class PaymentProfileDto(
    val id: String,
    val customerId: String,
    val reusable: Boolean,
    val default: Boolean,

    @field:ValidPaymentMethod
    val paymentMethod: PaymentMethod,

    val metadata: JsonNode? = null
)
