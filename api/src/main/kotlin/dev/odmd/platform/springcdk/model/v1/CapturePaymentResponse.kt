package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.TransactionStatus

data class CapturePaymentResponse(
    val paymentId: String,
    val status: TransactionStatus,
    val captured: CurrencyAmount?,
    val declineCode: String?,
    val declineReason: String?,
    val errors: List<ErrorResponse>?,
    val lineItemIds: List<String>?
)
