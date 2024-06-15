package dev.odmd.platform.springcdk.webhooks

import com.fasterxml.jackson.databind.ObjectMapper
import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.TransactionType
import dev.odmd.platform.springcdk.common.scaledLongToMonetaryAmount
import dev.odmd.platform.springcdk.common.toCurrencyAmount
import dev.odmd.platform.springcdk.domain.IdempotentRequestExecutor
import dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService
import dev.odmd.platform.springcdk.domain.entities.*
import dev.odmd.platform.springcdk.domain.repositories.LineItemRepository
import dev.odmd.platform.springcdk.domain.repositories.PaymentProfileRepository
import dev.odmd.platform.springcdk.domain.repositories.PaymentRepository
import dev.odmd.platform.springcdk.domain.repositories.PaymentTargetRepository
import dev.odmd.platform.springcdk.services.firstSuccessfulTransaction
import com.stripe.model.Charge
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.model.InvoiceLineItem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

internal const val STRIPE_PAYMENT_TARGET_TYPE = "stripeInvoice"
private const val LZ_CUSTOMER_ID_KEY = "lzCustomerId"
private const val UNICODE_SEPARATOR = '\u001D'

@Component
internal class InvoiceWebhookProcessor(
    private val paymentProfileRepository: PaymentProfileRepository,
    private val paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository,
    private val paymentTargetRepository: PaymentTargetRepository,
    private val idempotentRequestExecutor: IdempotentRequestExecutor,
    private val lineItemRepository: LineItemRepository,
    private val transitContext: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService
) {
    val logger: Logger = LoggerFactory.getLogger(InvoiceWebhookProcessor::class.java)
    val objectMapper = ObjectMapper()

    internal fun processInvoiceEvent(processInvoiceRequest: ProcessInvoiceRequest): Result<Unit> =
        runCatching {
            val paymentTarget = paymentTargetRepository.findOrCreatePaymentTarget(
                targetKey = processInvoiceRequest.invoice.id,
                targetType = STRIPE_PAYMENT_TARGET_TYPE
            )

            val lineItem = findOrCreateLineItem(processInvoiceRequest.invoice.lines.data.first())

            idempotentRequestExecutor.runOnce(processInvoiceRequest.event.id,
                alreadyRun = {
                    logger.info("Received already processed Webhook Event: [${processInvoiceRequest.event.id}")
                }) {
                val lzCustomerId = processInvoiceRequest.invoice.lines.data.first().metadata.getValue(LZ_CUSTOMER_ID_KEY)
                transitContext.scopedContext = lzCustomerId.encodeToByteArray()

                val eventBasedProperties =
                    EventBasedProperties.fromEventType(EventType.fromEventType(processInvoiceRequest.event.type))

                val payment = paymentTarget.payments.firstOrNull()

                if (payment == null) {
                    handleNewInvoice(
                        target = paymentTarget,
                        lineItem = lineItem,
                        invoice = processInvoiceRequest.invoice,
                        eventBasedProperties = eventBasedProperties,
                        event = processInvoiceRequest.event
                    )
                } else {
                    if (payment.firstSuccessfulTransaction() != null && eventBasedProperties.eventType == EventType.SUCCESS) {
                        throw AlreadyRecordedSuccess(payment.id)
                    }

                    handleUpdate(
                        invoice = processInvoiceRequest.invoice,
                        lineItem = lineItem,
                        payment = payment,
                        eventBasedProperties = eventBasedProperties,
                        event = processInvoiceRequest.event
                    )
                }

            }
        }.onFailure {
            logger.error("failed processing invoice webhook event", it)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, it.message, it)
        }

    private fun handleUpdate(
        invoice: Invoice,
        lineItem: LineItem,
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        eventBasedProperties: EventBasedProperties,
        event: Event,
    ) {
        val updatedPayment = createPaymentTransactions(
            payment = payment,
            lineItem = lineItem,
            eventBasedProperties = eventBasedProperties,
            invoice = invoice,
            event = event
        )
        paymentRepository.save(updatedPayment)
    }

    private fun handleNewInvoice(
        target: dev.odmd.platform.springcdk.domain.entities.PaymentTarget,
        lineItem: LineItem,
        invoice: Invoice,
        eventBasedProperties: EventBasedProperties,
        event: Event
    ) {
        val payment = dev.odmd.platform.springcdk.domain.entities.Payment(
            currency = invoice.currency,
            currencyAmount = longToCurrencyScaledBigDecimal(invoice.currency, invoice.amountDue),
            paymentDateTime = Instant.ofEpochSecond(invoice.created),
            requestedBy = "Stripe Subscription Webhook",
            externalPaymentId = UUID.randomUUID().toString(),
            paymentTarget = target,
            paymentMetadata = null,
            stoppedDateTime = null
        )
        target.payments.add(payment)

        val paymentMetadata = dev.odmd.platform.springcdk.domain.entities.PaymentMetadata(
            metadata = objectMapper.valueToTree(invoice.metadata),
            payment = payment
        )
        payment.paymentMetadata = paymentMetadata
        val updatedPayment = createPaymentTransactions(
            payment = payment,
            lineItem = lineItem,
            eventBasedProperties = eventBasedProperties,
            invoice = invoice,
            event = event
        )
        paymentRepository.save(updatedPayment)
    }


    private fun createPaymentTransactions(
        payment: dev.odmd.platform.springcdk.domain.entities.Payment,
        lineItem: LineItem,
        eventBasedProperties: EventBasedProperties,
        invoice: Invoice,
        event: Event
    ): dev.odmd.platform.springcdk.domain.entities.Payment {
        val lzCustomerId = invoice.lines.data.first().metadata.getValue(LZ_CUSTOMER_ID_KEY)
        val paymentProfile = paymentProfileRepository.findByCustomerId(lzCustomerId).firstOrNull {
            paymentProfileMatchesInvoice(it, invoice)
        } ?: throw NoProfileForCustomerId(lzCustomerId)

        val paymentTransaction = PaymentTransaction(
            currencyAmount = longToCurrencyScaledBigDecimal(invoice.currency, invoice.amountDue),
            transactionStatus = eventBasedProperties.transactionStatus,
            source = eventBasedProperties.eventType.value,
            paymentTransactionType = TransactionType.AUTH_CAPTURE,
            reason = invoice.billingReason,
            paymentProfile = paymentProfile,
            payment = payment,
            externalPaymentTransactionId = UUID.randomUUID().toString(),
            paymentTransactionMetadata = null
        )
        payment.paymentTransactions.add(paymentTransaction)

        val paymentTransactionMetadata = PaymentTransactionMetadata(
            metadata = objectMapper.valueToTree(invoice.metadata),
            paymentTransaction = paymentTransaction
        )

        paymentTransaction.paymentTransactionMetadata = paymentTransactionMetadata

        val paymentTransactionLineItem = dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem(
            currencyAmountApplied = lineItem.currencyAmount,
            lineItem = lineItem,
            paymentTransaction = paymentTransaction
        )
        paymentTransaction.lineItems.add(paymentTransactionLineItem)

        val gatewayPaymentTransaction = GatewayPaymentTransaction(
            gatewayTransactionType = GatewayTransactionType.AUTH_CAPTURE,
            gatewayIdentifier = invoice.charge,
            gatewayRequest = "",
            gatewayResponse = event,
            status = eventBasedProperties.transactionStatus.toString(),
            paymentTransaction = paymentTransaction
        )
        paymentTransaction.gatewayPaymentTransactions.add(gatewayPaymentTransaction)

        return payment
    }

    private fun findOrCreateLineItem(invoiceLineItem: InvoiceLineItem): LineItem =
        lineItemRepository.findOrCreateLineItem(
            currencyAmount = longToCurrencyScaledBigDecimal(invoiceLineItem.currency, invoiceLineItem.amount),
            externalLineItemId = invoiceLineItem.id,
            description = invoiceLineItem.description ?: "description not found"
        )
}

private fun paymentProfileMatchesInvoice(paymentProfile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile, invoice: Invoice): Boolean =
    when (val methodInformation = paymentProfile.methodInformation) {
        is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> {
            val charge = Charge.retrieve(invoice.charge)
            val paymentMethod = charge.paymentMethod
            methodInformation.creditCardInfo.gatewayToken.split(UNICODE_SEPARATOR).last() == paymentMethod
        }
    }

private fun longToCurrencyScaledBigDecimal(currency: String, value: Long) =
    scaledLongToMonetaryAmount(currency.uppercase(), value).toCurrencyAmount().amount

private data class EventBasedProperties(
    val eventType: EventType,
    val transactionStatus: TransactionStatus
) {
    companion object {
        fun fromEventType(eventType: EventType) = when (eventType) {
            EventType.ACTION_REQUIRED -> EventBasedProperties(eventType, TransactionStatus.PENDING_GATEWAY)
            EventType.SUCCESS -> EventBasedProperties(eventType, TransactionStatus.SUCCESS)
            EventType.FAILED -> EventBasedProperties(eventType, TransactionStatus.DECLINED)
        }
    }
}
