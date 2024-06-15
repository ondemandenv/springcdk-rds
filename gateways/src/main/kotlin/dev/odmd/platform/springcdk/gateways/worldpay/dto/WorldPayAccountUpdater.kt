package dev.odmd.platform.springcdk.gateways.worldpay.dto

import dev.odmd.platform.springcdk.common.CreditCardNetwork

data class WorldPayAccountUpdater(
    val originalCardInfo: WorldPayCardAccountInfo?,
    val newCardInfo: WorldPayCardAccountInfo?,
    val extendedCardMessage: WorldPayExtendedMessage?,
    val originalAccountInfo: WorldPayEcheckAccountInfo?,
    val newAccountInfo: WorldPayEcheckAccountInfo?,
    val originalCardTokenInfo: WorldPayTokenInfo?,
    val newCardTokenInfo: WorldPayTokenInfo?
)

data class WorldPayExtendedMessage(val message: String, val code: String)

data class WorldPayTokenInfo(
    val token: String,
    val type: CreditCardNetwork,
    val expYear: Int,
    val expMonth: Int,
    val firstDigit: Int
)