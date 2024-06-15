package dev.odmd.platform.springcdk.gateways

import dev.odmd.platform.springcdk.common.TransactionStatus
import java.time.Instant

sealed class GatewayResponse(
    val request: Any,
    val response: Any,
    val gatewayIdentifier: String,
    val status: TransactionStatus,
    val transactionDatetime: Instant,
    val declineCode: dev.odmd.platform.springcdk.gateways.DeclineCode?,
    val declineReason: dev.odmd.platform.springcdk.gateways.DeclineReason?
)
