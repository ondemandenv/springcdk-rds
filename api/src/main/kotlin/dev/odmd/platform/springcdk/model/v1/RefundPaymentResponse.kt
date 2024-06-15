package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.RefundType
import dev.odmd.platform.springcdk.common.TransactionStatus

data class RefundPaymentResponse(
    val requestId: String,
    val refundType: RefundType,
    val reason: String?,
    val lineItems: List<RefundResponseLineItem>?,
    val errors: List<dev.odmd.platform.springcdk.model.v1.ErrorResponse>?
) {
    data class RefundResponseLineItem(
        val id: String,
        val amount: CurrencyAmount,
        val status: TransactionStatus,
        val declineCode: String?,
        val declineReason: String?
    )
}
