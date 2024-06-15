package dev.odmd.platform.springcdk.model.v1

import com.fasterxml.jackson.annotation.JsonFormat
import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.TransactionType
import java.time.Instant

data class PaymentResponse(
    val paymentId: String,
    val paymentProfileId: String,
    val targetType: String,
    val targetKey: String,
    val requestedBy: String,
    val amount: CurrencyAmount,
    val lineItems: List<PaymentResponseLineItem>,
    val transactions: List<PaymentResponseTransaction>,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss", timezone = "UTC")
    val createdAt: Instant
) {
    data class PaymentResponseLineItem(
        val id: String,
        val amount: CurrencyAmount,
        val description: String,
        val relatedTransactionIds: List<String>
    )

    data class PaymentResponseTransaction(
        val id: String,
        val type: TransactionType,
        val amount: CurrencyAmount,
        val status: TransactionStatus,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss", timezone = "UTC")
        val createdAt: Instant
    )
}
