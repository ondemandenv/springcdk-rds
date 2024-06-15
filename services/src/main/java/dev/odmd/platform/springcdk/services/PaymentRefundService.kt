package dev.odmd.platform.springcdk.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NullNode
import dev.odmd.platform.springcdk.common.*
import dev.odmd.platform.springcdk.domain.IdempotentRequestExecutor
import dev.odmd.platform.springcdk.domain.entities.Payment
import dev.odmd.platform.springcdk.domain.entities.PaymentTransaction
import dev.odmd.platform.springcdk.domain.repositories.*
import dev.odmd.platform.springcdk.gateways.GatewayRefundResponse
import dev.odmd.platform.springcdk.model.v1.*
import dev.odmd.platform.springcdk.services.errors.PaymentNotFound
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import javax.persistence.EntityNotFoundException

@Service
class PaymentRefundService(
    private val paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository,
    private val lineItemRepository: LineItemRepository,
    private val paymentDSL: PaymentDSL,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val idempotentRequestExecutor: IdempotentRequestExecutor,
    private val gatewayPaymentTransactionRepository: GatewayPaymentTransactionRepository

) {

    companion object {
        private val logger: Logger = getLogger<PaymentRefundService>()
    }

    internal fun prepareRefund(paymentId: String, request: dev.odmd.platform.springcdk.model.v1.RefundPaymentRequest, customerId: String): dev.odmd.platform.springcdk.domain.entities.Payment {
        val payment = paymentRepository.findByExternalPaymentId(paymentId)
            ?: throw PaymentNotFound(paymentId)

        val originalPaymentProfile = payment.originalProfile()
            ?: throw EntityNotFoundException("Unable to find original payment method for customerId: ${customerId}, paymentId: ${paymentId}. Payment must have a successful capture transaction in order to be refunded.")

        if (originalPaymentProfile.customerId != customerId) {
            throw EntityNotFoundException("entityId does not match the customerId")
        }

        val lineItems = lineItemRepository.getAllByExternalLineItemIdIn(
            request.lineItems.map { it.id }.toSet()
        )

        val lineItemRefundAmounts = request.lineItems.map { refundLineItem ->
            val lineItem = lineItems.firstOrNull { it.externalLineItemId == refundLineItem.id }
                ?: throw InvalidRefundItem(refundLineItem.id)
            lineItem to refundLineItem.amount
        }

        val refundTransaction = idempotentRequestExecutor.performOnce(
            requestId = "${request.requestId}-prepare",
            alreadyPerformed = { refundTransactionId ->
                payment.paymentTransactions.first { it.id == refundTransactionId }
            }
        ) {
            var pendingRefund = paymentDSL.findOrCreatePendingRefund(
                payment,
                lineItemRefundAmounts,
                // TODO: add refund source request field
                "",
                request.reason,
                request.refundMetadata ?: JsonNodeFactory.instance.objectNode()
            )
            pendingRefund = paymentTransactionRepository.save(pendingRefund)
            pendingRefund.id to pendingRefund
        }

        return refundTransaction.payment
    }

    internal fun performRefund(
        requestId: RequestId,
        paymentPendingRefund: dev.odmd.platform.springcdk.domain.entities.Payment,
        mockedRequestResponse: MockRequestResponse? = null,
        refundMetadata: JsonNode? = null
    ): RefundPaymentResponse {
        val result: Pair<PaymentTransaction, dev.odmd.platform.springcdk.gateways.GatewayRefundResponse?> = idempotentRequestExecutor.performOnce(
            requestId = "${requestId}-perform",
            alreadyPerformed = { refundTransactionId ->
                paymentPendingRefund.paymentTransactions.first { it.id == refundTransactionId } to null
            }
        ) {
            val (refundGatewayTransaction, gatewayRefundResponse) = paymentDSL.refund(
                paymentPendingRefund,
                mockedRequestResponse,
                refundMetadata
            )

            paymentTransactionRepository.save(refundGatewayTransaction.paymentTransaction)

            refundGatewayTransaction.paymentTransaction.id to (refundGatewayTransaction.paymentTransaction to gatewayRefundResponse)
        }

        val transaction = result.first
        val refundResponse = result.second

        val errorList = if (transaction.transactionStatus == TransactionStatus.DECLINED) {
            listOf(
                dev.odmd.platform.springcdk.model.v1.ErrorResponse(
                    refundResponse?.declineCode?.toIntOrNull() ?: -1,
                    refundResponse?.declineReason ?: ""
                )
            )
        } else {
            emptyList()
        }

        return RefundPaymentResponse(
            requestId,
            // FIXME: we don't persist refund type anywhere. should we?
            RefundType.REVERSE,
            transaction.reason,
            transaction.lineItems.map {
                RefundPaymentResponse.RefundResponseLineItem(
                    it.lineItem.externalLineItemId,
                    CurrencyAmount(transaction.currencyAmount),
                    transaction.transactionStatus,
                    refundResponse?.declineCode,
                    refundResponse?.declineReason
                )
            },
            errorList
        )
    }

    internal fun recordRefundPayment(
        recordRefundRequest: dev.odmd.platform.springcdk.model.v1.RecordRefundRequest,
        lzEntityId: String
    ): ResponseEntity<Unit> {
        idempotentRequestExecutor.runOnce("${recordRefundRequest.targetKey + recordRefundRequest.targetValue}-recordRefund") {
            val gatewayIdentifier = recordRefundRequest.gatewayIdentifier
            val gatewayPaymentTransaction =
                gatewayPaymentTransactionRepository.findByGatewayIdentifier(gatewayIdentifier)
                    ?: throw PaymentGatewayTransactionNotFoundException(gatewayIdentifier)
            if (gatewayPaymentTransaction.paymentTransaction.lineItems.size != 1) {
                throw InvalidLineItemCount(gatewayPaymentTransaction.paymentTransaction.lineItems, 1)
            }
            val lineItemToRefund = gatewayPaymentTransaction.paymentTransaction.lineItems.first().lineItem
            val payment = gatewayPaymentTransaction.paymentTransaction.payment

            val refundTransaction =
                paymentDSL.findOrCreatePendingRefund(
                    payment = payment,
                    lineItemRefundAmounts = listOf(lineItemToRefund to recordRefundRequest.refundAmount),
                    source = recordRefundRequest.source,
                    reason = recordRefundRequest.reason,
                    transactionMetadata = recordRefundRequest.paymentTransactionMetadata ?: NullNode.instance,
                    forceValidBalance = false
                )

            refundTransaction.transactionStatus = TransactionStatus.SUCCESS
            paymentTransactionRepository.save(refundTransaction)
        }
        return ResponseEntity(HttpStatus.OK)
    }
}