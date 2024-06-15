package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.RefundType
import javax.validation.constraints.DecimalMin

data class RefundPaymentRequest(
    val requestId: dev.odmd.platform.springcdk.model.v1.RequestId,
    val refundType: RefundType,
    val reason: String,
    val lineItems: List<dev.odmd.platform.springcdk.model.v1.RefundPaymentRequest.RefundLineItem>,
    val refundMetadata: JsonNode? = null
) {
    data class RefundLineItem(
        val id: String,
        @field:DecimalMin("0.0")
        val amount: CurrencyAmount
    )
}
