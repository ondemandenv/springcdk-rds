package dev.odmd.platform.springcdk.model.v1

import javax.validation.constraints.NotBlank

data class CancelPaymentRequest(
    @field: NotBlank
    val requestId: RequestId,
    val reason: String,
    val lineItemIds: List<String>
)
