package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.TransactionStatus

data class AuthorizePaymentResponse(
    val targetKey: String,
    val requestId: RequestId,
    val targetType: String,
    val errors: List<ErrorResponse>?,
    val payments: List<AuthorizePaymentResponsePayment>?,
    val paymentProfiles: List<PaymentProfileDto>
) {

    data class AuthorizePaymentResponsePayment(
        val captured: Boolean,
        val paymentId: String,
        val status: TransactionStatus,
        val declineCode: String?,
        val declineReason: String?,
        val authorized: CurrencyAmount?,
        val lineItemIds: List<String>,
        val profileId: String
    )
}
