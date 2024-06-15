import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ondemand.platform.payments.common.TransactionStatus.DECLINED
import com.ondemand.platform.payments.common.TransactionStatus.SUCCESS
import com.ondemand.platform.payments.dbmigration.DbMigrationInitializer
import com.ondemand.platform.payments.dbmigration.PostgresContainerInitializer
import com.ondemand.platform.payments.domain.cryptography.*
import com.ondemand.platform.payments.domain.entities.GatewayTransactionType
import com.ondemand.platform.payments.domain.repositories.*
import com.ondemand.platform.payments.gateways.PaymentGatewayService
import com.ondemand.platform.payments.gateways.PaymentGatewaySupplier
import dev.odmd.platform.springcdk.webhooks.InvoiceWebhookProcessor
import dev.odmd.platform.springcdk.webhooks.ProcessInvoiceRequest
import dev.odmd.platform.springcdk.webhooks.STRIPE_PAYMENT_TARGET_TYPE
import com.stripe.model.Charge
import com.stripe.model.Event
import com.stripe.model.Invoice
import com.stripe.net.ApiResource
import dev.odmd.platform.springcdk.domain.cryptography.*
import org.jetbrains.kotlin.konan.file.use
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Propagation

private fun withMockedCharge(function: (MockedStatic<Charge>) -> Unit) {
    Mockito.mockStatic(Charge::class.java).use { mockedCharge ->
        mockedCharge.`when`<Charge> {
            Charge.retrieve(any())
        }.doReturn(Charge().apply { paymentMethod = "card_1234" })
        function(mockedCharge)
    }
}

@EnableDomainData
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.transaction.annotation.Transactional(propagation = Propagation.NEVER)
@ContextConfiguration(
    initializers = [
        PostgresContainerInitializer::class,
        DbMigrationInitializer::class
    ],
    classes = [
        dev.odmd.platform.springcdk.domain.cryptography.NoopCryptographyService::class,
        InvoiceWebhookProcessor::class,
        PaymentTargetRepository::class,
        LineItemRepository::class,
        TestIdempotentRequestExecutorConfig::class
    ]
)
@TestPropertySource(properties = [
    /*
    Enable lazy loading outside of transactions, since these tests aren't automatically wrapped in a db transaction
     */
    "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true"
])
@TestExecutionListeners(
    listeners = [
        ResetTransitConfigBeforeTest::class,
        ResetPaymentDataListener::class
    ],
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS
)
internal class InvoiceWebhookProcessorIntegrationTest {
    @Autowired
    private lateinit var paymentProfileRepository: PaymentProfileRepository

    @Autowired
    private lateinit var paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository

    @Autowired
    private lateinit var paymentTargetRepository: PaymentTargetJpaRepository

    @MockBean
    lateinit var scopedTransitContextService: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService

    @MockBean
    lateinit var transitContextService: dev.odmd.platform.springcdk.domain.cryptography.TransitContextService

    @MockBean
    lateinit var paymentGatewayService: PaymentGatewayService

    @MockBean
    private lateinit var paymentGatewayFactory: PaymentGatewaySupplier

    val objectMapper = ObjectMapper().findAndRegisterModules()

    @Autowired
    private lateinit var invoiceWebhookProcessor: InvoiceWebhookProcessor

    @Autowired
    private lateinit var testIdempotentRequestExecutor: TestIdempotentRequestExecutor

    @BeforeEach
    fun beforeEach() {
        whenever(transitContextService.getContext()).thenReturn("cus1334".encodeToByteArray())
        stubIdempotentRequest()
    }

    @Test
    fun `GIVEN a new invoice payload WHEN the user has a valid payment profile THEN a new payment is created`() {
        val jsonFile =
            InvoiceWebhookProcessorIntegrationTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val event = ApiResource.GSON.fromJson(jsonFile, Event::class.java)
        val invoice = event.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = invoice.lines.data.first().metadata.getValue("lzCustomerId")

        val paymentProfile = getPaymentProfile(customerId = lzCustomerId)
        paymentProfileRepository.save(paymentProfile)

        withMockedCharge {
            val result = invoiceWebhookProcessor.processInvoiceEvent(
                ProcessInvoiceRequest(
                    invoice, event
                )
            )
            assert(result.isSuccess)
        }

        val savedPaymentTarget = paymentTargetRepository.findByTargetKeyAndTargetType(
            targetKey = invoice.id,
            targetType = STRIPE_PAYMENT_TARGET_TYPE
        )
        val savedPayment = savedPaymentTarget!!.payments.first()
        assert(savedPaymentTarget.payments.size == 1)
        assert(savedPayment.paymentTransactions.size == 1)
        assert(savedPayment.currency == invoice.currency)
        assert(savedPayment.currencyAmount.longValueExact() == invoice.amountDue / 100)
        assert(savedPayment.paymentTarget.id == savedPaymentTarget.id)

        val savedPaymentTransaction = savedPayment.paymentTransactions.first()
        assert(savedPaymentTransaction.currencyAmount.longValueExact() == invoice.amountDue / 100)
        assert(savedPaymentTransaction.source == event.type)
        assert(savedPaymentTransaction.transactionStatus == SUCCESS)
        assert(savedPaymentTransaction.reason == invoice.billingReason)

        val savedPaymentTransactionLineItem = savedPaymentTransaction.lineItems.first()
        assert(savedPaymentTransaction.lineItems.size == 1)
        assert(savedPaymentTransactionLineItem.currencyAmountApplied.longValueExact() == invoice.amountDue / 100)

        val savedLineItem = savedPaymentTransactionLineItem.lineItem
        assert(savedLineItem.externalLineItemId == invoice.lines.data.first().id)
        assert(savedLineItem.currencyAmount.longValueExact() == invoice.amountDue / 100)

        val savedGatewayPaymentTransaction = savedPaymentTransaction.gatewayPaymentTransactions.first()
        assert(savedPaymentTransaction.gatewayPaymentTransactions.size == 1)
        assert(savedGatewayPaymentTransaction.gatewayTransactionType == GatewayTransactionType.AUTH_CAPTURE)
        assert(savedGatewayPaymentTransaction.status == "SUCCESS")
        assert(savedGatewayPaymentTransaction.gatewayIdentifier == invoice.charge)
        assert(savedGatewayPaymentTransaction.gatewayRequest == objectMapper.valueToTree(""))
        verifySavedGatewayResponse(savedGatewayPaymentTransaction.gatewayResponse, event)
    }

    @Test
    fun `GIVEN a new invoice payload WHEN a PaymentTarget for the invoice exists THEN a new PaymentTransaction is created`() {
        val paymentFailedJsonFile =
            InvoiceWebhookProcessorIntegrationTest::class.java.getResource("invoice_payment_failed.json")?.readText()
        val failedEvent = ApiResource.GSON.fromJson(paymentFailedJsonFile, Event::class.java)
        val failedInvoice = failedEvent.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val lzCustomerId = failedInvoice.lines.data.first().metadata.getValue("lzCustomerId")
        val paymentProfile = getPaymentProfile(customerId = lzCustomerId)
        paymentProfileRepository.save(paymentProfile)

        withMockedCharge {
            invoiceWebhookProcessor.processInvoiceEvent(
                ProcessInvoiceRequest(
                    failedInvoice, failedEvent
                )
            )
        }

        val savedPaymentTarget = paymentTargetRepository.findByTargetKeyAndTargetType(
            targetKey = failedInvoice.id,
            targetType = STRIPE_PAYMENT_TARGET_TYPE
        )
        val savedPayment = savedPaymentTarget!!.payments.first()
        assert(savedPaymentTarget.payments.size == 1)
        assert(savedPayment.paymentTransactions.size == 1)
        val savedPaymentTransactionDeclined = savedPayment.paymentTransactions.last()
        assert(savedPaymentTransactionDeclined.currencyAmount.longValueExact() == failedInvoice.amountDue / 100)
        assert(savedPaymentTransactionDeclined.transactionStatus == DECLINED)
        assert(savedPaymentTransactionDeclined.source == failedEvent.type)
        assert(savedPaymentTransactionDeclined.reason == failedInvoice.billingReason)
        assert(savedPaymentTransactionDeclined.lineItems.size == 1)

        val paymentSuccessJson =
            InvoiceWebhookProcessorIntegrationTest::class.java.getResource("invoice_payment_succeeded.json")?.readText()
        val paymentSucceededEvent = ApiResource.GSON.fromJson(paymentSuccessJson, Event::class.java)
        val paymentSuccessInvoice = paymentSucceededEvent.dataObjectDeserializer.deserializeUnsafe() as Invoice
        withMockedCharge {
            invoiceWebhookProcessor.processInvoiceEvent(
                ProcessInvoiceRequest(
                    paymentSuccessInvoice, paymentSucceededEvent
                )
            )
        }
        val savedPaymentTargetSuccess = paymentTargetRepository.findByTargetKeyAndTargetType(
            targetKey = failedInvoice.id,
            targetType = STRIPE_PAYMENT_TARGET_TYPE
        )
        val savedPaymentSuccess = savedPaymentTargetSuccess!!.payments.first()
        assert(savedPaymentTargetSuccess.payments.size == 1)
        assert(savedPaymentSuccess.paymentTransactions.size == 2)

        val savedPaymentTransactionSuccess = savedPaymentSuccess.paymentTransactions.last()
        assert(savedPaymentTransactionSuccess.currencyAmount.longValueExact() == paymentSuccessInvoice.amountDue / 100)
        assert(savedPaymentTransactionSuccess.transactionStatus == SUCCESS)
        assert(savedPaymentTransactionSuccess.source == paymentSucceededEvent.type)
        assert(savedPaymentTransactionSuccess.reason == failedInvoice.billingReason)
        assert(savedPaymentTransactionSuccess.lineItems.size == 1)
        assert(savedPaymentTransactionSuccess.paymentTransactionMetadata?.metadata == ObjectMapper().valueToTree(paymentSuccessInvoice.metadata))
        val savedPaymentTransactionLineItem = savedPaymentTransactionSuccess.lineItems.first()
        assert(savedPaymentTransactionLineItem.currencyAmountApplied.longValueExact() == paymentSuccessInvoice.amountDue / 100)

        val savedLineItem = savedPaymentTransactionLineItem.lineItem
        assert(savedLineItem.externalLineItemId == paymentSuccessInvoice.lines.data.first().id)
        assert(savedLineItem.currencyAmount.longValueExact() == paymentSuccessInvoice.amountDue / 100)

        val savedGatewayPaymentTransaction = savedPaymentTransactionSuccess.gatewayPaymentTransactions.first()
        assert(savedPaymentTransactionDeclined.gatewayPaymentTransactions.size == 1)
        assert(savedGatewayPaymentTransaction.gatewayTransactionType == GatewayTransactionType.AUTH_CAPTURE)
        assert(savedGatewayPaymentTransaction.status == "SUCCESS")
        assert(savedGatewayPaymentTransaction.gatewayIdentifier == failedInvoice.charge)
        assert(savedGatewayPaymentTransaction.gatewayRequest == objectMapper.valueToTree(""))
        verifySavedGatewayResponse(savedGatewayPaymentTransaction.gatewayResponse, paymentSucceededEvent)
    }

    private fun verifySavedGatewayResponse(gatewayResponse: JsonNode, event: Event) {
        /*
        Comparing with round-trip-serialized Event because:

            - as part of storing, we convert from snake_case to camelCase
            - guarantees consistent number types (int vs. long, decimal vs. double)
         */
        val expectedJsonNode = objectMapper
            .valueToTree<JsonNode>(event)
            .let(objectMapper::writeValueAsString)
            .let(objectMapper::readTree)
        assert(gatewayResponse == expectedJsonNode)
    }

    private fun stubIdempotentRequest(
        performAction: Boolean = true,
    ) = testIdempotentRequestExecutor.stubRun(null, performAction)
}
