package dev.odmd.platform.springcdk.gateways.worldpay.dto

data class WorldPayAuthorizationResponse(
    val cnpTxnId: Long,
    val response: String,
    val orderId: String?,
    val cardProductId: String?,
    val message: String?,
    val location: String?,
    val approvedAmount: Long?,
    val cnpToken: String?,
    val tokenMessage: String?,
    val tokenResponseCode: String?,
    val giftCardResponse: WorldPayGiftCardResponse?,
    val accountUpdater: WorldPayAccountUpdater?,
    val cardSuffix: String?,
    val paymentAccountReferenceNumber: String?,
    val checkoutId: String?,
    val bin: String?,
    val authCode : String?,
    val avsResult: String?
)
