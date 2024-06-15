package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.CurrencyAmount
import javax.validation.Valid


data class PaymentDto(

    @field:Valid
    @field:dev.odmd.platform.springcdk.model.v1.ValidPaymentMethod
    val paymentMethod: dev.odmd.platform.springcdk.model.v1.PaymentMethod,

    val lineItemIds: List<String>,
    val shouldCapture: Boolean,
    val authorizeAmount: CurrencyAmount,
    @Deprecated(message = "metadata has been moved up to the transaction level")
    val paymentMetadata: JsonNode
)
