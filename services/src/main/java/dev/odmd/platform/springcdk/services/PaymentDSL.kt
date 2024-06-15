package dev.odmd.platform.springcdk.services

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.*
import dev.odmd.platform.springcdk.domain.entities.*
import dev.odmd.platform.springcdk.gateways.*
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseStatus
import java.math.BigDecimal
import java.util.*

@Service
class PaymentDSL(
    private val gatewaySupplier: PaymentGatewaySupplier,
    private val logger: Logger = getLogger<PaymentDSL>()
) {


    fun findOrCreatePendingAuthorization(
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        profile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile,
        lineItems: List<LineItem>,
        currency: Currency,
        shouldCapture: Boolean,
        source: String,
        reason: String,
        requestId: String,
        paymentTransactionMetadata: JsonNode
    ): PaymentTransaction {
        val transactionType = if (shouldCapture) {
            TransactionType.AUTH_CAPTURE
        } else {
            TransactionType.AUTH
        }

        val hasAnySuccessfulAuth =
            payment
                .paymentTransactions
                .any {
                    TransactionType.ALL_AUTH_TYPES.contains(it.paymentTransactionType) &&
                            it.transactionStatus == TransactionStatus.SUCCESS
                }

        if (hasAnySuccessfulAuth) {
            throw PaymentAlreadyAuthorizedException(payment.externalPaymentId)
        }

        return payment.findPendingTransaction(setOf(transactionType)) ?: PaymentTransaction(
            UUID.randomUUID().toString(),
            transactionType,
            source,
            payment.currencyAmount,
            TransactionStatus.PENDING,
            payment,
            profile,
            reason,
            paymentTransactionMetadata = null
        ).also { paymentTransaction ->

            payment.paymentTransactions.add(paymentTransaction)

            PaymentTransactionMetadata(
                metadata = paymentTransactionMetadata,
                paymentTransaction
            ).also { paymentTransaction.paymentTransactionMetadata = it }

            lineItems.map {
                dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem(
                    currencyAmountApplied = it.currencyAmount,
                    paymentTransaction = paymentTransaction,
                    lineItem = it
                )
            }.also { transactionLineItems ->
                paymentTransaction.lineItems.addAll(transactionLineItems)
            }
        }
    }

    fun authorize(
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        billingDescriptor: String,
        mockedRequestResponse: MockRequestResponse? = null
    ): Pair<GatewayPaymentTransaction, dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse> {
        val pendingAuthTransaction = payment.getPendingTransactionOrThrow(TransactionType.ALL_AUTH_TYPES)

        val shouldCapture = pendingAuthTransaction.paymentTransactionType == TransactionType.AUTH_CAPTURE

        val authRequest = when (val methodInformation = pendingAuthTransaction.paymentProfile.methodInformation) {
            is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> {
                dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest(
                    pendingAuthTransaction.externalPaymentTransactionId,
                    pendingAuthTransaction.monetaryAmount,
                    pendingAuthTransaction.paymentProfile.customerId,
                    payment.paymentTarget.targetKey,
                    methodInformation.creditCardInfo.gatewayToken,
                    methodInformation.creditCardInfo.expMonth,
                    methodInformation.creditCardInfo.expYear,
                    methodInformation.billingInformation,
                    pendingAuthTransaction.paymentTransactionMetadata?.metadata,
                    shouldCapture,
                    billingDescriptor,
                    methodInformation.creditCardInfo.creditCardNetwork
                )
            }
        }

        return updatingTransaction(pendingAuthTransaction, mockedRequestResponse) {
            authorize(authRequest)
        }
    }

    fun throwUnlessCanRefundLineItems(payment: dev.odmd.platform.springcdk.domain.entities.Payment, lineItemRefundAmounts: List<Pair<LineItem, CurrencyAmount>>) {
        val balances = payment.totalBalanceByLineItem().withDefault { Balance.ZERO }

        lineItemRefundAmounts.forEach { (lineItem, refundAmount) ->
            val lineItemBalance = balances[lineItem.externalLineItemId] ?: Balance.ZERO
            if (!lineItemBalance.canRefund(refundAmount.amount)) {
                throw InvalidRefundLineItemBalance(
                    lineItemExternalId = lineItem.externalLineItemId,
                    balance = lineItemBalance,
                    attemptedRefund = refundAmount.amount
                )
            }
        }
    }

    fun findOrCreatePendingRefund(
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        lineItemRefundAmounts: List<Pair<LineItem, CurrencyAmount>>,
        source: String,
        reason: String,
        transactionMetadata: JsonNode,
        forceValidBalance: Boolean = true
    ): PaymentTransaction {
        try {
            throwUnlessCanRefundLineItems(payment, lineItemRefundAmounts)
        } catch (e: InvalidRefundLineItemBalance) {
            if (forceValidBalance) {
                throw e
            }
            logger.error("recordRefund.insufficientBalance", e)
        }

        val totalRefundAmount = lineItemRefundAmounts.sumOf { it.second.amount }

        return payment.findPendingTransaction(setOf(TransactionType.REFUND)) ?: PaymentTransaction(
            UUID.randomUUID().toString(),
            TransactionType.REFUND,
            source,
            totalRefundAmount,
            TransactionStatus.PENDING,
            payment,
            payment.originalProfile() ?: throw NoRefundPaymentProfileFound(payment.externalPaymentId),
            reason,
            paymentTransactionMetadata = null
        ).also { refundTransaction ->
            refundTransaction.lineItems.addAll(
                lineItemRefundAmounts.map { (lineItem, amount) ->
                    dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem(
                        currencyAmountApplied = amount.amount,
                        paymentTransaction = refundTransaction,
                        lineItem = lineItem
                    )
                }
            )
            payment.paymentTransactions.add(refundTransaction)
            PaymentTransactionMetadata(
                metadata = transactionMetadata,
                paymentTransaction = refundTransaction
            ).also { refundTransaction.paymentTransactionMetadata = it }
        }
    }

    fun refund(
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        mockedRequestResponse: MockRequestResponse? = null,
        refundMetadata: JsonNode? = null
    ): Pair<GatewayPaymentTransaction, dev.odmd.platform.springcdk.gateways.GatewayRefundResponse> {
        val pendingRefundTransaction = payment.getPendingTransactionOrThrow(setOf(TransactionType.REFUND))

        val transactionToRefund = payment.firstSuccessfulCaptureTransaction()
        val approvedGatewayTransaction = transactionToRefund?.approvedGatewayTransaction()
        if (transactionToRefund == null || approvedGatewayTransaction == null) {
            throw TransactionToReverseNotFound(payment.externalPaymentId)
        }

        return updatingTransaction(pendingRefundTransaction, mockedRequestResponse) {
            refund(
                dev.odmd.platform.springcdk.gateways.GatewayRefundRequest(
                    customerId = transactionToRefund.paymentProfile.customerId,
                    targetKey = payment.paymentTarget.targetKey,
                    gatewayRequestId = pendingRefundTransaction.externalPaymentTransactionId,
                    refundType = RefundType.REVERSE,
                    refundAmount = pendingRefundTransaction.monetaryAmount,
                    gatewayIdentifierToRefund = approvedGatewayTransaction.gatewayIdentifier,
                    refundMetadata = refundMetadata
                )
            )
        }
    }

    internal fun <R> updatingTransaction(
        paymentTransaction: PaymentTransaction,
        mockedRequestResponse: MockRequestResponse? = null,
        performTransaction: PaymentGatewayService.() -> R,
    ): Pair<GatewayPaymentTransaction, R>
            where R : dev.odmd.platform.springcdk.gateways.GatewayResponse {
        val gatewayId = paymentTransaction.paymentProfile.gatewayIdentifier
        val gatewayResponse = gatewaySupplier.get(gatewayId, mockedRequestResponse).performTransaction()

        paymentTransaction.transactionStatus = gatewayResponse.status

        // TODO: consolidate transaction types
        val gatewayTransactionType = when (paymentTransaction.paymentTransactionType) {
            TransactionType.AUTH -> GatewayTransactionType.AUTH
            TransactionType.AUTH_CAPTURE -> GatewayTransactionType.AUTH_CAPTURE
            TransactionType.REFUND -> GatewayTransactionType.REFUND
            TransactionType.CAPTURE -> GatewayTransactionType.CAPTURE
        }

        val gatewayTransaction = GatewayPaymentTransaction(
            gatewayTransactionType,
            gatewayResponse.status.name,
            gatewayResponse.request,
            gatewayResponse.response,
            paymentTransaction,
            gatewayResponse.gatewayIdentifier
        )
        paymentTransaction.gatewayPaymentTransactions.add(gatewayTransaction)

        return gatewayTransaction to gatewayResponse
    }
}

fun dev.odmd.platform.springcdk.domain.entities.Payment.firstSuccessfulCaptureTransaction(): PaymentTransaction? =
    paymentTransactions.firstOrNull {
        (it.paymentTransactionType == TransactionType.CAPTURE ||
                it.paymentTransactionType == TransactionType.AUTH_CAPTURE) &&
                it.transactionStatus == TransactionStatus.SUCCESS
    }

fun PaymentTransaction.approvedGatewayTransaction() =
    gatewayPaymentTransactions.firstOrNull {
        it.status == TransactionStatus.SUCCESS.toString()
                && (it.gatewayTransactionType == GatewayTransactionType.AUTH_CAPTURE ||
                it.gatewayTransactionType == GatewayTransactionType.CAPTURE)
    }

fun dev.odmd.platform.springcdk.domain.entities.Payment.findPendingTransaction(transactionTypes: Set<TransactionType>) =
    paymentTransactions.firstOrNull {
        it.transactionStatus == TransactionStatus.PENDING &&
                transactionTypes.contains(it.paymentTransactionType)
    }

fun dev.odmd.platform.springcdk.domain.entities.Payment.getPendingTransactionOrThrow(transactionTypes: Set<TransactionType>) =
    findPendingTransaction(transactionTypes)
        ?: throw PaymentTransactionNotPendingException(transactionTypes, externalPaymentId)

fun dev.odmd.platform.springcdk.domain.entities.Payment.firstSuccessfulTransaction(): PaymentTransaction? =
    paymentTransactions.firstOrNull {
        (it.paymentTransactionType == TransactionType.AUTH ||
                it.paymentTransactionType == TransactionType.AUTH_CAPTURE) &&
                it.transactionStatus == TransactionStatus.SUCCESS
    }

fun dev.odmd.platform.springcdk.domain.entities.Payment.originalProfile(): dev.odmd.platform.springcdk.domain.entities.PaymentProfile? =
    firstSuccessfulTransaction()?.paymentProfile

/**
 * Errors
 */
class NoRefundPaymentProfileFound(paymentExternalId: String) :
    RuntimeException("Refund requires a prior successful AUTH or AUTH_CAPTURE transaction. Payment: $paymentExternalId")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidRefundLineItemBalance(
    val lineItemExternalId: String,
    val balance: Balance,
    val attemptedRefund: BigDecimal
) : RuntimeException("Line item $lineItemExternalId cannot be refunded $attemptedRefund because its balance is $balance")


@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidLineItemCount(
    lineItems: MutableSet<dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem>,
    lineItemCount: Int
) : RuntimeException("Line item size ${lineItems.size} is not equal to ${lineItemCount}. LineItemIds = ${lineItems.map { it.lineItem.externalLineItemId }}")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidRefundItem(
    itemId: String
) : RuntimeException("Invalid refund Line item $itemId")

class PaymentTransactionNotPendingException(
    types: Set<TransactionType>,
    paymentExternalId: String
) : RuntimeException("Cannot perform transaction because payment $paymentExternalId has no pending transaction of any following type: $types.")

@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
class UnsupportedAsyncPaymentsException(
    val status: TransactionStatus,
    val requestId: String
) : RuntimeException("Transaction failed for payment $requestId due to Transaction status being $status as platform does not support Asynchronous Transactions")

class TransactionToReverseNotFound(val paymentId: String) :
    RuntimeException("Cannot refund payment for paymentId: $paymentId .")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class PaymentAlreadyAuthorizedException(val paymentId: String) :
    RuntimeException("Invalid attempt to authorize payment $paymentId which already has a successful authorize transaction.")
