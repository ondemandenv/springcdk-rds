package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.RefundType

data class GetPaymentRefundOptionsResponse(
    val lineItems: List<dev.odmd.platform.springcdk.model.v1.GetPaymentRefundOptionsResponse.RefundOptionLineItem>

) {
    data class RefundOptionLineItem(
        val id: String,
        val paymentId: String,
        val maximumAmount: CurrencyAmount,
        val refundOptions: List<RefundType>
    )
}
