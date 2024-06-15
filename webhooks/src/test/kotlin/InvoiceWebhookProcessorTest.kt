import com.ondemand.platform.payments.common.TransactionStatus
import com.ondemand.platform.payments.common.scaledLongToMonetaryAmount
import com.ondemand.platform.payments.common.toCurrencyAmount
import com.ondemand.platform.payments.domain.IdempotentRequestExecutor
import dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService
import com.ondemand.platform.payments.domain.entities.LineItem
import dev.odmd.platform.springcdk.domain.entities.PaymentTarget
import com.ondemand.platform.payments.domain.repositories.*
import dev.odmd.platform.springcdk.webhooks.InvoiceWebhookProcessor
import dev.odmd.platform.springcdk.webhooks.ProcessInvoiceRequest
import dev.odmd.platform.springcdk.webhooks.STRIPE_PAYMENT_TARGET_TYPE
import com.stripe.model.Charge
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.net.ApiResource
import fixtures.getPayment
import fixtures.getPaymentTarget
import fixtures.getPaymentTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException


internal class InvoiceWebhookProcessorTest {

    private val paymentProfileRepository: PaymentProfileRepository = mockk()
    private val paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository = mockk()
    private val paymentTargetRepository: PaymentTargetRepository = mockk()
    private val lineItemRepository: LineItemRepository = mockk()
    private val idempotentRequestExecutor: IdempotentRequestExecutor = mockk()
    private val transitContext: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService = mockk(relaxed = true)

    private val invoiceWebhookProcessor =
        InvoiceWebhookProcessor(
            paymentProfileRepository = paymentProfileRepository,
            paymentRepository = paymentRepository,
            paymentTargetRepository = paymentTargetRepository,
            lineItemRepository = lineItemRepository,
            idempotentRequestExecutor = idempotentRequestExecutor,
            transitContext = transitContext,
        )

    @BeforeEach
    fun beforeEach() {
        val functionToExecute = slot<() -> Unit>()
        every { idempotentRequestExecutor.runOnce(any(), any(), capture(functionToExecute)) } answers {
            functionToExecute.captured.invoke()
        }
        every { paymentRepository.save(any()) } returns null
        mockkStatic(Charge::class)
        every { Charge.retrieve(any()) } returns Charge().apply { paymentMethod = "card_1234" }
    }

    @Test
    fun `GIVEN a new invoice payload WHEN the user has a valid payment profile THEN a new payment is created `() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")
        val emptyTarget = dev.odmd.platform.springcdk.domain.entities.PaymentTarget(
            targetKey = invoice.id,
            targetType = STRIPE_PAYMENT_TARGET_TYPE
        )

        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns listOf(getPaymentProfile(customerId = lzCustomerId))
        every { paymentTargetRepository.findOrCreatePaymentTarget(any(), STRIPE_PAYMENT_TARGET_TYPE) } returns emptyTarget
        stubLineItemForInvoice(invoice)

        val result = invoiceWebhookProcessor.processInvoiceEvent(
            ProcessInvoiceRequest(
                invoice, event
            )
        )

        verify(exactly = 1) {
            idempotentRequestExecutor.runOnce(event.id, any(), any())
            paymentRepository.save(any())
        }
        assert(result.isSuccess)
    }

    @Test
    fun `GIVEN a new invoice payload WHEN there are multiple PaymentProfiles for a user THEN the PaymentProfile associated with the charge is used`() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")
                ?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")
        val expectedPaymentMethodId = "sample_default_method_1" // same as json file

        val configuredPaymentProfiles = listOf(
            getPaymentProfile(customerId = lzCustomerId),
            getPaymentProfile(
                customerId = lzCustomerId,
                paymentMethodInformation = getPaymentMethodInformation(
                    creditCardInformation = getCreditCardInfo(
                        gatewayToken = expectedPaymentMethodId
                    )
                )
            )
        )

        // without this no way to do assertions that payment was changed since no Payment is returned by invoiceProcessor
        val existingPaymentTarget = getPaymentTargetWithDeclinedTransaction()
        val existingPayment = existingPaymentTarget.payments.first()
        assert(existingPayment.paymentTransactions.size == 1)

        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns configuredPaymentProfiles
        every {
            paymentTargetRepository.findOrCreatePaymentTarget(
                any(),
                STRIPE_PAYMENT_TARGET_TYPE
            )
        } returns existingPaymentTarget
        every { Charge.retrieve(any()) } returns Charge().apply { paymentMethod = expectedPaymentMethodId }
        stubLineItemForInvoice(invoice)

        val result = invoiceWebhookProcessor.processInvoiceEvent(
            ProcessInvoiceRequest(
                invoice, event
            )
        )
        verify(exactly = 1) {
            idempotentRequestExecutor.runOnce(event.id, any(), any())
            paymentRepository.save(any())
        }
        assert(existingPayment.paymentTransactions.size == 2)
        assert(existingPaymentTarget.payments.size == 1)
        assert(existingPayment.paymentTransactions.last().paymentProfile.id == configuredPaymentProfiles.last().id)
        assert(result.isSuccess)
    }

    @Test
    fun `GIVEN an invoice event payload WHEN a PaymentTarget for the invoice already exists THEN a new PaymentTransaction is added to the existing Payment`() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")


        val existingPaymentTarget = getPaymentTarget(payments = null)
        val existingPayment = getPayment(
            paymentTransactions = null,
            paymentTarget = existingPaymentTarget
        )
        existingPaymentTarget.payments.add(existingPayment)
        val paymentTransaction = getPaymentTransaction(existingPayment, transactionStatus = TransactionStatus.DECLINED)
        existingPayment.paymentTransactions.add(paymentTransaction)

        assert(existingPayment.paymentTransactions.size == 1)
        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns listOf(getPaymentProfile(customerId = lzCustomerId))
        every {
            paymentTargetRepository.findOrCreatePaymentTarget(
                any(),
                STRIPE_PAYMENT_TARGET_TYPE
            )
        } returns existingPaymentTarget
        stubLineItemForInvoice(invoice)

        val result = invoiceWebhookProcessor.processInvoiceEvent(
            ProcessInvoiceRequest(
                invoice, event
            )
        )

        assert(existingPayment.paymentTransactions.size == 2)
        assert(existingPaymentTarget.payments.size == 1)
        verify(exactly = 1) {
            idempotentRequestExecutor.runOnce(event.id, any(), any())
            paymentRepository.save(any())
        }

        assert(result.isSuccess)
    }

    @Test
    fun `GIVEN an invoice event payload WHEN a there is no PaymentProfile THEN an error is thrown`() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")

        val existingPaymentTarget = getPaymentTarget()
        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns listOf()
        every {
            paymentTargetRepository.findOrCreatePaymentTarget(
                any(),
                STRIPE_PAYMENT_TARGET_TYPE
            )
        } returns existingPaymentTarget
        stubLineItemForInvoice(invoice)

        assertThrows<ResponseStatusException> {
            invoiceWebhookProcessor.processInvoiceEvent(
                ProcessInvoiceRequest(
                    invoice, event
                )
            )
        }
        verify(exactly = 0) {
            paymentProfileRepository.findByCustomerId(lzCustomerId)
            paymentRepository.save(any())
        }
    }

    @Test
    fun `GIVEN an invoice paid payload WHEN a there is already a successful PaymentTransaction THEN an error is thrown`() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")

        val existingPaymentTarget = getPaymentTarget()
        val existingPayment = existingPaymentTarget.payments.first()
        assert(existingPayment.paymentTransactions.size == 1)
        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns listOf(getPaymentProfile(customerId = lzCustomerId))
        every {
            paymentTargetRepository.findOrCreatePaymentTarget(
                any(),
                STRIPE_PAYMENT_TARGET_TYPE
            )
        } returns existingPaymentTarget
        stubLineItemForInvoice(invoice)

        assertThrows<ResponseStatusException> {
            invoiceWebhookProcessor.processInvoiceEvent(
                ProcessInvoiceRequest(
                    invoice, event
                )
            )
        }

        verify(exactly = 1) {
            idempotentRequestExecutor.runOnce(event.id, any(), any())
        }
        verify(exactly = 0) {
            paymentRepository.save(any())
        }
    }

    @Test
    fun `GIVEN an invoice paid payload WHEN a there is already a failed PaymentTransaction THEN the payload is processed`() {
        val jsonFile =
            InvoiceWebhookProcessorTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")

        val existingPaymentTarget = getPaymentTargetWithDeclinedTransaction()
        val existingPayment = existingPaymentTarget.payments.first()
        assert(existingPayment.paymentTransactions.size == 1)
        every { paymentProfileRepository.findByCustomerId(lzCustomerId) } returns listOf(getPaymentProfile(customerId = lzCustomerId))
        every {
            paymentTargetRepository.findOrCreatePaymentTarget(
                any(),
                STRIPE_PAYMENT_TARGET_TYPE
            )
        } returns existingPaymentTarget
        stubLineItemForInvoice(invoice)

        invoiceWebhookProcessor.processInvoiceEvent(ProcessInvoiceRequest(invoice, event))

        assert(existingPayment.paymentTransactions.size == 2)
        assert(existingPaymentTarget.payments.size == 1)

        verify(exactly = 1) {
            idempotentRequestExecutor.runOnce(event.id, any(), any())
            paymentRepository.save(any())
        }
    }

    private fun getPaymentTargetWithDeclinedTransaction(): dev.odmd.platform.springcdk.domain.entities.PaymentTarget {
        val existingPaymentTarget = getPaymentTarget(payments = null)
        val existingPayment = getPayment(
            paymentTransactions = null,
            paymentTarget = existingPaymentTarget
        )
        existingPaymentTarget.payments.add(existingPayment)
        val paymentTransaction = getPaymentTransaction(existingPayment, transactionStatus = TransactionStatus.DECLINED)
        existingPayment.paymentTransactions.add(paymentTransaction)
        return existingPaymentTarget
    }

    private fun stubLineItemForInvoice(invoice: Invoice): LineItem {
        val invoiceLineItem = invoice.lines.data.first()
        return LineItem(
            externalLineItemId = invoiceLineItem.id,
            currencyAmount = scaledLongToMonetaryAmount(invoiceLineItem.currency.uppercase(), invoiceLineItem.amount).toCurrencyAmount().amount,
            description = invoiceLineItem.description
        ).also { lineItem ->
            every {
                lineItemRepository.findOrCreateLineItem(
                    externalLineItemId = eq(lineItem.externalLineItemId),
                    currencyAmount = eq(lineItem.currencyAmount),
                    description = eq(lineItem.description)
                )
            } returns lineItem
        }
    }
}
