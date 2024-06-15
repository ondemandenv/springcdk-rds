package dev.odmd.platform.springcdk.services.utils.mapper

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.domain.entities.LineItem
import dev.odmd.platform.springcdk.model.v1.PaymentResponse

fun LineItem.toPaymentLineItemResponse(transactionIds: List<String>): PaymentResponse.PaymentResponseLineItem {
    return PaymentResponse.PaymentResponseLineItem(
        externalLineItemId,
        CurrencyAmount(currencyAmount),
        description,
        transactionIds
    )
}
