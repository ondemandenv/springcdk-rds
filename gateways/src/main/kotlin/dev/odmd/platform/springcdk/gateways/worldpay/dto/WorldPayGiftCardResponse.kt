package dev.odmd.platform.springcdk.gateways.worldpay.dto

data class WorldPayGiftCardResponse(
    val sequenceNumber: String?,
    val availableBalance: String?,
    val beginningBalance: String?,
    val endingBalance: String?,
    val cashBackAmount: String?
)
