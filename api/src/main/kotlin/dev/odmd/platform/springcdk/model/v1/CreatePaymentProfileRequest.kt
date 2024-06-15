package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.Valid


data class CreatePaymentProfileRequest(
    @field:Schema(type = "string", example = "replaceme")
    val requestId: dev.odmd.platform.springcdk.model.v1.RequestId,
    @field:Schema(type = "boolean", example = "false")
    val validate: Boolean,
    val reusable: Boolean,
    val default: Boolean,
    @field:Valid
    val method: dev.odmd.platform.springcdk.model.v1.PaymentProfilePaymentMethod,
    val metadata: JsonNode?
)
