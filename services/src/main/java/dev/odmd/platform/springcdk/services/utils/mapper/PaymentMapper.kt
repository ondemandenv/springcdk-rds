package dev.odmd.platform.springcdk.services.utils.mapper

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.domain.entities.LineItem
import dev.odmd.platform.springcdk.domain.entities.Payment
import dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem
import dev.odmd.platform.springcdk.model.v1.PaymentResponse

fun dev.odmd.platform.springcdk.domain.entities.Payment.toPaymentResponse(
    lineItems: List<LineItem>,
    paymentTransactionLineItems: List<dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem>
): PaymentResponse {
    return PaymentResponse(
        externalPaymentId,
        paymentTransactions.firstOrNull()?.paymentProfile?.externalId ?: "",
        paymentTarget.targetType,
        paymentTarget.targetKey,
        requestedBy,
        CurrencyAmount(currencyAmount),
        mapPaymentTransactionLineItems(lineItems, paymentTransactionLineItems),
        paymentTransactions.sortedBy { it.createdDateTime }.map { it.toPaymentResponseTransaction() },
        createdDateTime
    )
}

private fun mapPaymentTransactionLineItems(
    lineItems: List<LineItem>,
    paymentTransactionLineItems: List<dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem>
): List<PaymentResponse.PaymentResponseLineItem> =
    lineItems.map { li ->
        li.toPaymentLineItemResponse(paymentTransactionLineItems.filter { ptli ->
            ptli.lineItem.externalLineItemId == li.externalLineItemId
        }.map { it -> it.paymentTransaction.externalPaymentTransactionId })
    }.toSet().toList()


