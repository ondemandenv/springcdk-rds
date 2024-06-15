package dev.odmd.platform.springcdk.gateways.worldpay.dto

data class WorldPayCreditResponse(
    val cnpTxnId: Long,
    val response: String,
    val message: String?,
    val location: String?,
    val cnpToken: String?,
    val tokenMessage: String?,
    val checkoutId: String?
)
