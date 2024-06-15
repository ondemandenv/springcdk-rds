package dev.odmd.platform.springcdk.gateways

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.RefundType
import dev.odmd.platform.springcdk.common.TransactionStatus
import java.time.Instant
import javax.money.MonetaryAmount

class GatewayRefundRequest(
    val customerId: String,
    val targetKey: String,
    val gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    val refundType: RefundType,
    val refundAmount: MonetaryAmount,
    val gatewayIdentifierToRefund: String,
    val refundMetadata: JsonNode? = null
)

class GatewayRefundResponse(
    val refundType: RefundType,
    val refundAmount: MonetaryAmount,
    request: Any,
    response: Any,
    gatewayIdentifier: String,
    status: TransactionStatus,
    transactionDatetime: Instant,
    declineCode: dev.odmd.platform.springcdk.gateways.DeclineCode?,
    declineReason: dev.odmd.platform.springcdk.gateways.DeclineReason?
): dev.odmd.platform.springcdk.gateways.GatewayResponse(request, response, gatewayIdentifier, status, transactionDatetime, declineCode, declineReason)
