package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.TransactionType
import dev.odmd.platform.springcdk.common.isZero
import java.math.BigDecimal


data class Balance(
    val authorizedAmount: BigDecimal,
    val capturedAmount: BigDecimal,
    val refundedAmount: BigDecimal
) {
    init {
        assert(authorizedAmount >= BigDecimal.ZERO)
        assert(capturedAmount >= BigDecimal.ZERO)
        assert(refundedAmount >= BigDecimal.ZERO)
    }

    companion object {
        val ZERO = Balance(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)

        fun authorized(amount: BigDecimal) =
            Balance(
                authorizedAmount = amount,
                capturedAmount = BigDecimal.ZERO,
                refundedAmount = BigDecimal.ZERO
            )

        fun captured(amount: BigDecimal) =
            Balance(
                authorizedAmount = BigDecimal.ZERO,
                capturedAmount = amount,
                refundedAmount = BigDecimal.ZERO
            )

        fun refunded(amount: BigDecimal) =
            Balance(
                authorizedAmount = BigDecimal.ZERO,
                capturedAmount = BigDecimal.ZERO,
                refundedAmount = amount
            )

        fun from(
            transactionStatus: TransactionStatus,
            paymentTransactionType: TransactionType,
            amount: BigDecimal
        ) =
            if (transactionStatus != TransactionStatus.SUCCESS) {
                ZERO
            } else {
                when (paymentTransactionType) {
                    TransactionType.AUTH -> authorized(amount)
                    TransactionType.AUTH_CAPTURE -> Balance(
                        authorizedAmount = amount,
                        capturedAmount = amount,
                        refundedAmount = BigDecimal.ZERO
                    )
                    TransactionType.CAPTURE -> captured(amount)
                    TransactionType.REFUND -> refunded(amount)
                }
            }
    }

    operator fun plus(other: Balance) =
        Balance(
            authorizedAmount = this.authorizedAmount + other.authorizedAmount,
            capturedAmount = this.capturedAmount + other.capturedAmount,
            refundedAmount = this.refundedAmount + other.refundedAmount
        )

    fun canRefund(amount: BigDecimal): Boolean =
        amount > BigDecimal.ZERO && (capturedAmount - refundedAmount) >= amount

    fun canCapture(amount: BigDecimal): Boolean =
        amount > BigDecimal.ZERO && capturedAmount.isZero && authorizedAmount >= amount
}

fun Payment.totalBalanceByLineItem(): Map<String, Balance> =
    paymentTransactions
        .map(PaymentTransaction::balanceByLineItem)
        .fold(mutableMapOf()) { acc, lineItemBalances ->
            acc.apply {
                lineItemBalances.forEach { (lineItemId, lineItemTransactionBalance) ->
                    merge(lineItemId, lineItemTransactionBalance, Balance::plus)
                }
            }
        }

fun PaymentTransaction.balanceByLineItem(): Map<String, Balance> =
    lineItems.associateBy({ it.lineItem.externalLineItemId }) {
        Balance.from(
            transactionStatus,
            paymentTransactionType,
            it.currencyAmountApplied
        )
    }
