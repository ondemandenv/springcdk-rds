package dev.odmd.platform.springcdk.services.utils.mapper

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.domain.entities.PaymentTransaction
import dev.odmd.platform.springcdk.model.v1.PaymentResponse

fun PaymentTransaction.toPaymentResponseTransaction(): PaymentResponse.PaymentResponseTransaction {
    return PaymentResponse.PaymentResponseTransaction(
        externalPaymentTransactionId,
        paymentTransactionType,
        CurrencyAmount(currencyAmount),
        transactionStatus,
        createdDateTime
    )
}
