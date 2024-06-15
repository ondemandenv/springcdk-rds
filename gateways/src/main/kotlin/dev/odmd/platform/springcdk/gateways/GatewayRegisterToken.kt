package dev.odmd.platform.springcdk.gateways

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.CreditCardNumber

class GatewayRegisterTokenRequest(
    val gatewayRequestId: dev.odmd.platform.springcdk.gateways.GatewayRequestId,
    val creditCardNumber: CreditCardNumber,
    val cardVerificationValue: String?,
    val expMonth: Int,
    val expYear: Int,
    val customerId: String,
    val billingInformation: BillingInformation?,
    val metadata: JsonNode?
)

class GatewayRegisterTokenResponse(
    val token: String,
    val responseCode: String,
    val responseMessage: String,
    val gatewayTransactionIdentifier: String
)
