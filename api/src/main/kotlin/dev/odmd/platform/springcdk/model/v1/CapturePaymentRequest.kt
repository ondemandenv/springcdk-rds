package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import javax.validation.constraints.DecimalMin

data class CapturePaymentRequest(
    val requestId: RequestId,
    @field:DecimalMin("0.0")
    val amount: CurrencyAmount,
    val reason: String,
    val lineItemIds: List<String>
)
