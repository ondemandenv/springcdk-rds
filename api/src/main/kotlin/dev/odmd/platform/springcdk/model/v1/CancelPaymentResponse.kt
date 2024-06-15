package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.TransactionStatus

data class CancelPaymentResponse(
    val paymentId: String,
    val status: TransactionStatus,
    val declineCode: String?,
    val declineReason: String?,
    val lineItems: List<String>?
)
