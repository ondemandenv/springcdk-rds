package dev.odmd.platform.springcdk.model.v1

data class StopPaymentRequest(
    val requestId: RequestId,
    val reason: String
)
