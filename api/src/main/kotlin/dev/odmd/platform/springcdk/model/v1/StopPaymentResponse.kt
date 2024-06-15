package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.TransactionStatus

data class StopPaymentResponse(
    val paymentId: String,
    val status: TransactionStatus
)
