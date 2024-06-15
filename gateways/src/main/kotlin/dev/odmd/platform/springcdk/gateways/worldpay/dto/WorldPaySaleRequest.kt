package dev.odmd.platform.springcdk.gateways.worldpay.dto

data class WorldPaySaleRequest(
    val gatewayRequestId: String,
    val cnpTxnId: Long?,
    val customerId: String,
    val orderId: String?,
    val amount: Long,
    val orderSource: String?,
    val cnpToken: String?,
    val tokenExpDate: String?,
    val state: String?,
    val city: String?,
    val descriptor: String?,
    val customerReference: String?
)
