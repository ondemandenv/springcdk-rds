package dev.odmd.platform.springcdk.gateways

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.common.CreditCardNumber
import dev.odmd.platform.springcdk.common.TransactionStatus
import java.time.Instant
import javax.money.MonetaryAmount

class GatewayAuthorizeResponse(
    request: Any,
    response: Any,
    gatewayIdentifier: String,
    status: TransactionStatus,
    transactionDatetime: Instant,
    val amountAuthorized: MonetaryAmount,

    declineCode: dev.odmd.platform.springcdk.gateways.DeclineCode?,
    declineReason: dev.odmd.platform.springcdk.gateways.DeclineReason?,
    val token: String?,
) : dev.odmd.platform.springcdk.gateways.GatewayResponse(
    request, response, gatewayIdentifier,
    status, transactionDatetime, declineCode, declineReason
)

sealed class GatewayAuthorizeRequest(
    val customerId: String,
    val gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    val authorizeAmount: MonetaryAmount,
    val metadata: JsonNode?,
    val shouldCapture: Boolean,
    val targetKey: String?
)

class GatewayCreditCardAuthorizeRequest(
    customerId: String,
    gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    authorizeAmount: MonetaryAmount,
    targetKey: String?,
    val creditCardNumber: CreditCardNumber,
    val expMonth: Int,
    val expYear: Int,
    val billingInformation: BillingInformation?,
    metadata: JsonNode?,
    shouldCapture: Boolean,
    @Suppress("unused") val billingDescriptor: String
) : dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest(
    customerId = customerId,
    gatewayRequestId = gatewayRequestId,
    authorizeAmount = authorizeAmount,
    metadata = metadata,
    shouldCapture = shouldCapture,
    targetKey = targetKey
)

class GatewayCreditCardTokenAuthorizeRequest(
    gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    authorizeAmount: MonetaryAmount,
    customerId: String,
    targetKey: String?,
    val token: String,
    val expMonth: Int,
    val expYear: Int,
    val billingInformation: BillingInformation?,
    metadata: JsonNode?,
    shouldCapture: Boolean,
    val billingDescriptor: String,
    val creditCardNetwork: CreditCardNetwork
) : dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest(
    customerId = customerId,
    gatewayRequestId = gatewayRequestId,
    authorizeAmount = authorizeAmount,
    metadata = metadata,
    shouldCapture = shouldCapture,
    targetKey = targetKey
)
