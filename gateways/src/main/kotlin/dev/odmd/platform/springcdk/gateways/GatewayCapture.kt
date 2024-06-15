package dev.odmd.platform.springcdk.gateways

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.TransactionStatus
import java.time.Instant
import javax.money.MonetaryAmount

data class GatewayCaptureRequest(
    val gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    val authorizationReference: dev.odmd.platform.springcdk.gateways.AuthorizationReference,
    val captureAmount: MonetaryAmount,
    val metadata: JsonNode?
)

class GatewayCaptureResponse(
    request: Any,
    response: Any,
    gatewayIdentifier: String,
    status: TransactionStatus,
    transactionDatetime: Instant,
    declineCode: dev.odmd.platform.springcdk.gateways.DeclineCode?,
    declineReason: dev.odmd.platform.springcdk.gateways.DeclineReason?,
    val authorizationReference: dev.odmd.platform.springcdk.gateways.AuthorizationReference
) : dev.odmd.platform.springcdk.gateways.GatewayResponse(
    request, response, gatewayIdentifier,
    status, transactionDatetime, declineCode, declineReason
)
