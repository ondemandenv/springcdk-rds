package dev.odmd.platform.springcdk.gateways.worldpay.dto

data class WorldPayEcheckAccountInfo(
    val accNum: String?,
    val routingNum: String?,
    val accountType: String?
)
