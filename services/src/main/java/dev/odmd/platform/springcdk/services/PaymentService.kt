package dev.odmd.platform.springcdk.services

import dev.odmd.platform.springcdk.common.*
import dev.odmd.platform.springcdk.domain.IdempotentRequestExecutor
import dev.odmd.platform.springcdk.domain.entities.*
import dev.odmd.platform.springcdk.domain.repositories.*
import dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse
import dev.odmd.platform.springcdk.model.v1.*
import dev.odmd.platform.springcdk.services.errors.Invalid0AuthInput
import dev.odmd.platform.springcdk.services.errors.InvalidAuthCaptureInput
import dev.odmd.platform.springcdk.services.utils.mapper.toPaymentProfileDto
import dev.odmd.platform.springcdk.services.utils.mapper.toPaymentResponse
import org.slf4j.MDC
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.*


@Service
/**
 * Dedicated API for interacting with payments & transactions.
 *
 * All transactions should follow a two-phase commit strategy as follows:
 *
 * 1. Pre-commit pending entities to repositories
 * 2. Perform gateway call and upsert entities with gateway response
 *
 * This is done to ensure that we always have a persistent record that a gateway transaction was
 * requested, even if an exception occurs.
 */
class PaymentService(
    private val paymentDSL: PaymentDSL,
    private val paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository,
    private val paymentTransactionRepository: PaymentTransactionRepository,
    private val profileService: PaymentProfileService,
    private val targetRepository: PaymentTargetRepository,
    private val lineItemRepository: LineItemRepository,
    private val idempotentRequestExecutor: IdempotentRequestExecutor,
    private val paymentRefundService: PaymentRefundService
) {

    /* PaymentService needs to know whether the profile is active to support these two behaviors:
       1. Preventing transactions on inactive profiles, by passing requireActiveProfile = true which will fail to find inactive profiles.
       2. Allowing transactions on inactive profiles, by passing requireActiveProfile = false which will only find inactive profiles (pending validation).
    */
    fun authorizePayment(
        request: AuthorizePaymentRequest,
        gatewayIdentifier: String? = null,
        customerId: String,
        mockedRequestResponse: MockRequestResponse? = null,
        requireActiveProfile: Boolean = true
    ): AuthorizePaymentResponse {
        validateAuthRequest(request)

        val paymentsAndProfiles = MDC.putCloseable("paymentRequestId", request.requestId).use {
            try {
                profileService.getOrCreateProfilesForPayments(customerId, request.paymentDtos, gatewayIdentifier, requireActiveProfile, mockedRequestResponse)
            } catch (e: Exception) {
                throw InvalidAuthCaptureInput(
                    request.requestId,
                    "Failed to get or create profiles for authorization: ${e.message}",
                    e
                )
            }
        }
        val paymentsPendingAuth = prepareAuth(
            request,
            paymentsAndProfiles,
            gatewayIdentifier,
            customerId
        )
        return performAuth(
            request.requestId,
            paymentsPendingAuth,
            request.billingDescriptor ?: Constants.defaultBillingDescriptor,
            request.paymentDtos[0].shouldCapture,
            request.targetKey,
            customerId,
            mockedRequestResponse
        )
    }

    fun getZeroDollarAuthPaymentMethod(paymentDtos: List<dev.odmd.platform.springcdk.model.v1.PaymentDto>): dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod? {
        return if (paymentDtos.count() == 1 &&
            paymentDtos.any {
                it.authorizeAmount.amount.isZero &&
                        !it.shouldCapture &&
                        it.paymentMethod.type == dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.CREDIT_CARD
            }
        ) {
            paymentDtos.first().paymentMethod.creditCard!!
        } else {
            null
        }
    }

    private fun validateAuthRequest(request: dev.odmd.platform.springcdk.model.v1.AuthorizePaymentRequest) {
        if (request.paymentDtos.all { it.authorizeAmount.amount.isZero } &&
            request.lineItemDtos.isNotEmpty()) {
            // requiring empty line items for $0 auth to avoid complexity w/ line item balance validation
            throw Invalid0AuthInput(request.requestId, "Cannot specify line items when authorizing 0.")
        }
        request.lineItemDtos.filter { it.amount.amount < BigDecimal.ZERO }.let {
            if (it.isNotEmpty()) {
                throw InvalidAuthCaptureInput(
                    request.requestId,
                    "Line item amount must be >= 0. Invalid line items: $it"
                )
            }
        }

        request.lineItemDtos
            .filter { requestLineItem ->
                val countPaymentsWithLineItem = request.paymentDtos.count { payment ->
                    payment.lineItemIds.contains(requestLineItem.id)
                }
                countPaymentsWithLineItem != 1
            }
            .let {
                if (it.isNotEmpty()) {
                    throw InvalidAuthCaptureInput(
                        request.requestId,
                        "All line items must be associated with 1 payment. LineItems referenced by multiple payments: $it. Payments: ${request.paymentDtos}"
                    )
                }
            }

        request.paymentDtos.filter { paymentDto ->
            val paymentLineItemAmounts =
                request
                    .lineItemDtos
                    .filter { paymentDto.lineItemIds.contains(it.id) }
                    .map { it.amount }
            val totalLineItemAmount = paymentLineItemAmounts.fold(CurrencyAmount(BigDecimal.ZERO)) { a1, a2 -> a1 + a2 }
            paymentDto.authorizeAmount != totalLineItemAmount
        }.let {
            if (it.isNotEmpty()) {
                throw InvalidAuthCaptureInput(
                    request.requestId,
                    "Payments must authorize total of applied line item amounts. Invalid payments: $it"
                )
            }
        }
    }

    internal fun prepareAuth(
        request: dev.odmd.platform.springcdk.model.v1.AuthorizePaymentRequest,
        paymentsAndProfiles: List<Pair<PaymentDto, PaymentProfile>>,
        gatewayIdentifier: String?,
        customerId: String
    ): List<dev.odmd.platform.springcdk.domain.entities.Payment> {
        val target = targetRepository.findOrCreatePaymentTarget(
            targetKey = request.targetKey,
            targetType = request.targetType
        )

        val lineItemEntities =
            request.lineItemDtos.map {
                lineItemRepository.findOrCreateLineItem(
                    externalLineItemId = it.id,
                    description = it.description,
                    currencyAmount = it.amount.amount
                )
            }

        return idempotentRequestExecutor.performOnce(
            requestId = "${request.requestId}-prepare",
            alreadyPerformed = {
                /*
                 if pending transaction was already found/created, don't attempt again.
                 the perform step will either:
                   - perform the pending transaction if it remains
                   - throw because no pending transaction is there
                   - if already performed, return the same result
                */
                target.payments.toList()
            }
        ) {
            target.id to findOrCreatePaymentWithPendingAuth(
                target = target,
                lineItemEntities = lineItemEntities,
                request = request,
                paymentsAndProfiles = paymentsAndProfiles
            )
        }
    }

    private fun findOrCreatePaymentWithPendingAuth(
        target: dev.odmd.platform.springcdk.domain.entities.PaymentTarget,
        lineItemEntities: List<LineItem>,
        request: dev.odmd.platform.springcdk.model.v1.AuthorizePaymentRequest,
        paymentsAndProfiles: List<Pair<PaymentDto, PaymentProfile>>,
    ): List<Payment> {
        val payments = paymentsAndProfiles.map { (paymentDto, profile) ->
            val payment =
                target.payments.firstOrNull { payment ->
                    val paymentLineItemExternalIds = payment.paymentTransactions.flatMap { ptx ->
                        ptx.lineItems.map { it.lineItem.externalLineItemId }
                    }.toSet()
                    paymentLineItemExternalIds == paymentDto.lineItemIds.toSet() &&
                            payment.currencyAmount == paymentDto.authorizeAmount.amount
                } ?: dev.odmd.platform.springcdk.domain.entities.Payment(
                    externalPaymentId = UUID.randomUUID().toString(),
                    paymentDateTime = Instant.now(),
                    stoppedDateTime = null,
                    // TODO: set to current user
                    requestedBy = request.source,
                    currency = request.currencyCode,
                    currencyAmount = paymentDto.authorizeAmount.amount,
                    paymentTarget = target,
                    paymentMetadata = null
                ).also { payment ->
                    target.payments.add(payment)

                    dev.odmd.platform.springcdk.domain.entities.PaymentMetadata(
                        payment = payment,
                        metadata = paymentDto.paymentMetadata
                    ).also {
                        payment.paymentMetadata = it
                    }
                }

            paymentDSL.findOrCreatePendingAuthorization(
                payment,
                profile,
                lineItemEntities,
                Currency.getInstance(request.currencyCode),
                paymentDto.shouldCapture,
                request.source,
                request.reason,
                request.requestId,
                paymentDto.paymentMetadata
            )
            payment
        }
        return paymentRepository.saveAll(payments)
    }

    internal fun performAuth(
        requestId: RequestId,
        paymentsPendingAuth: List<Payment>,
        billingDescriptor: String,
        shouldCapture: Boolean = false,
        targetKey: String,
        customerId: String,
        mockedRequestResponse: MockRequestResponse? = null
    ): AuthorizePaymentResponse {
        // all payments should share the same target
        val paymentTarget = paymentsPendingAuth.first().paymentTarget

        val results: List<Pair<GatewayPaymentTransaction, GatewayAuthorizeResponse?>> = idempotentRequestExecutor.performOnce(
            requestId = "${requestId}-perform",
            alreadyPerformed = {
                /*
                affectedEntityId is a Long, so we can only reliably store a single ID as a side effect of performing the idempotent action.

                as a result, our alreadyPerformed logic just returns the payments' auth/auth_capture transactions, which should result in the transactions that were created when the action was performed. however, it is possible that subsequent calls have been made that have added new transactions to the payments (e.g. auth decline -> auth approved)
                 */
                paymentsPendingAuth.flatMap { payment ->
                    payment.paymentTransactions
                        .toList()
                        .filter {
                            TransactionType.ALL_AUTH_TYPES.contains(it.paymentTransactionType)
                        }.map {
                            it.gatewayPaymentTransactions.last() to null
                        }
                }
            }
        ) {
            val results = paymentsPendingAuth.map {
                paymentDSL.authorize(it, billingDescriptor, mockedRequestResponse)
            }

            paymentTransactionRepository.saveAll(results.map { it.first.paymentTransaction })

            paymentTarget.id to results
        }

        val paymentProfilesDTO = results.map { it.first.paymentTransaction.paymentProfile.toPaymentProfileDto() }

        return AuthorizePaymentResponse(
            paymentTarget.targetKey,
            requestId,
            paymentTarget.targetType,
            results
                .filter { it.first.paymentTransaction.transactionStatus != TransactionStatus.SUCCESS }
                .map { (_, response) ->
                    //todo: error code integer?
                    ErrorResponse(
                        code = response?.declineCode?.toIntOrNull() ?: -1,
                        description = response?.declineReason ?: ""
                    )
                },
            results
                .sortedBy {
                    it.first.createdDateTime
                }
                .map { (gatewayTransaction, gatewayResponse) ->
                    AuthorizePaymentResponse.AuthorizePaymentResponsePayment(
                        gatewayTransaction.gatewayTransactionType == GatewayTransactionType.AUTH_CAPTURE,
                        gatewayTransaction.paymentTransaction.payment.externalPaymentId,
                        gatewayTransaction.paymentTransaction.transactionStatus,
                        // TODO: response code & reason should be available on all gateway txs
                        gatewayResponse?.declineCode,
                        gatewayResponse?.declineReason,
                        CurrencyAmount(gatewayTransaction.paymentTransaction.currencyAmount),
                        gatewayTransaction
                            .paymentTransaction
                            .lineItems
                            .map { it.lineItem.externalLineItemId }
                            .sorted(),
                        gatewayTransaction.paymentTransaction.paymentProfile.externalId
                    )
                },
            paymentProfilesDTO
        )
    }

    fun refundPayment(
        paymentId: String,
        refundPaymentRequest: dev.odmd.platform.springcdk.model.v1.RefundPaymentRequest,
        customerId: String,
        mockedRequestResponse: MockRequestResponse? = null
    ): RefundPaymentResponse {
        val pendingRefund = paymentRefundService.prepareRefund(paymentId, refundPaymentRequest, customerId)

        return paymentRefundService.performRefund(refundPaymentRequest.requestId, pendingRefund, mockedRequestResponse, refundPaymentRequest.refundMetadata)
    }

    fun recordRefundPayment(
        recordRefundRequest: dev.odmd.platform.springcdk.model.v1.RecordRefundRequest,
        lzEntityId: String
    ): ResponseEntity<Unit> {
        return paymentRefundService.recordRefundPayment(recordRefundRequest, lzEntityId)
    }

    fun getAllPaymentsForTarget(
        targetType: String,
        targetKey: String
    ): List<PaymentResponse> {
        val paymentsByTarget = try {
            targetRepository.getPaymentTarget(
                targetKey = targetKey,
                targetType = targetType
            )
        } catch (e: PaymentTargetNotFoundException) {
            return emptyList()
        }
        return paymentsByTarget.payments.map { getPaymentResponseFromPayment(it) }
    }

    fun getSinglePayment(paymentId: String): PaymentResponse? =
        paymentRepository.findByExternalPaymentId(paymentId)?.let { getPaymentResponseFromPayment(it) }

    private fun getPaymentResponseFromPayment(payment: dev.odmd.platform.springcdk.domain.entities.Payment): PaymentResponse {
        val paymentTransactionLineItems =
            payment.paymentTransactions.flatMap { it.lineItems }

        val lineItems = paymentTransactionLineItems.map { it.lineItem }
        return payment.toPaymentResponse(lineItems, paymentTransactionLineItems)
    }

    fun getPaymentsByCustomerId(customerId: String, page: Int, size: Int): Page<PaymentResponse> {
        val pageReq = PageRequest.of(page, size, Sort.by("createdDateTime").descending())
        val payments = paymentRepository.findPaymentsByCustomerId(customerId, pageReq)

        return payments.map { getPaymentResponseFromPayment(it) }
    }
}
