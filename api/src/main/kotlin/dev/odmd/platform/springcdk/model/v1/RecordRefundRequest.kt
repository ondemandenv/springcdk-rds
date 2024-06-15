package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.odmd.platform.springcdk.common.CurrencyAmount
import javax.validation.constraints.DecimalMin

data class RecordRefundRequest(
    val gatewayIdentifier: String,
    val reason: String,
    val paymentTransactionMetadata: JsonNode? = JsonNodeFactory.instance.objectNode(),
    @field:DecimalMin("0.0")
    val refundAmount: CurrencyAmount,
    val targetKey: String,
    val targetValue: String,
    val source: String
)