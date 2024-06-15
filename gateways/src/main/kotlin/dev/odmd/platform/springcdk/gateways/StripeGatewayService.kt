package dev.odmd.platform.springcdk.gateways

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.convertValue
import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.common.scaledLongToMonetaryAmount
import dev.odmd.platform.springcdk.common.toScaledLong
import com.stripe.Stripe
import com.stripe.exception.CardException
import com.stripe.model.Card
import com.stripe.model.Charge
import com.stripe.model.Customer
import com.stripe.net.RequestOptions
import com.stripe.param.ChargeCreateParams
import com.stripe.param.CustomerCreateParams
import com.stripe.param.CustomerCreateParams.Address
import com.stripe.param.CustomerSearchParams
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*

@Service
// Class is marked `final` to avoid warning due to using non-final class members in the `init` block
internal final class StripeGatewayService(
    config: StripeConfiguration
) : PaymentGateway() {
    companion object {
        private val logger = LoggerFactory.getLogger(StripeGatewayService::class.java)
    }

    init {
        if (config.isApiKeyInitialized) {
            config.validateApiKey()
            Stripe.apiKey = config.apiKey
        } else {
            logger.info("No Stripe API key configured, skipping initialization.")
        }
    }

    override fun authorize(request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        when (request) {
            is dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest -> {
                logger.info("Credit card authorization request: ${request.authorizeAmount}")
                throw ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "CreditCardAuthorizeRequest without token not implemented yet"
                )
            }

            is dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest -> {
                return authorizeWithToken(request)
            }
        }
    }

    private fun authorizeWithToken(request: dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        val stripeToken = dev.odmd.platform.springcdk.gateways.StripeToken.fromToken(request.token)
        val stripeCustomerId = stripeToken.stripeCustomerId
        val token = stripeToken.paymentMethodId

        val chargeCreateParams = ChargeCreateParams.builder()
            .setAmount(request.authorizeAmount.toScaledLong())
            .setCapture(request.shouldCapture)
            .setCurrency(request.authorizeAmount.currency.toString().lowercase())
            .setSource(token)
            .setCustomer(stripeCustomerId)
            .setStatementDescriptorSuffix(request.billingDescriptor) // descriptor phone cannot be set per transaction
            .build()

        val charge = catchingCardException("Unable to create Stripe payment Charge", request.gatewayRequestId) {
            Charge.create(chargeCreateParams)
        }

        val chargeStatus = charge.status
        val authResponse: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse
        MDC.putCloseable("gatewayRequestId", request.gatewayRequestId).use {
            when (chargeStatus) {
                "succeeded" -> {
                    //success!!
                    logger.event(
                        "stripe.successfulResponse", mapOf(
                            "stripeTransactionStatus" to TransactionStatus.SUCCESS,
                            "stripeChargeStatus" to charge.status,
                            "stripe" to charge.captured
                        )
                    )
                    authResponse = authGatewayBuilder(request, charge, TransactionStatus.SUCCESS, token)
                    return authResponse
                }

                "pending" -> {
                    // Should rarely happen for card transaction
                    logger.event(
                        "stripe.pendingResponse", mapOf(
                            "stripeTransactionStatus" to TransactionStatus.PENDING_GATEWAY,
                            "stripeChargeStatus" to charge.status,
                            "stripe" to charge.captured
                        )
                    )
                    authResponse = authGatewayBuilder(request, charge, TransactionStatus.PENDING_GATEWAY, token)
                    return authResponse
                }

                "failed" -> {
                    logger.event(
                        "stripe.failedResponse", mapOf(
                            "stripeTransactionStatus" to TransactionStatus.DECLINED,
                            "stripeChargeStatus" to charge.status,
                            "stripe" to charge.captured
                        )
                    )
                    authResponse = authGatewayBuilder(request, charge, TransactionStatus.DECLINED, token)
                    return authResponse
                }

                else -> {
                    logger.event(
                        "stripe.elseResponse", mapOf(
                            "stripeTransactionStatus" to TransactionStatus.PENDING_GATEWAY,
                            "stripeChargeStatus" to charge.status,
                            "stripe" to charge.captured
                        )
                    )
                    authResponse = authGatewayBuilder(request, charge, TransactionStatus.PENDING_GATEWAY, token)
                    return authResponse
                }
            }
        }

    }

    override fun capture(captureRequest: dev.odmd.platform.springcdk.gateways.GatewayCaptureRequest): dev.odmd.platform.springcdk.gateways.GatewayCaptureResponse {
        TODO("Not yet implemented")
    }

    override fun refund(refundRequest: dev.odmd.platform.springcdk.gateways.GatewayRefundRequest): dev.odmd.platform.springcdk.gateways.GatewayRefundResponse {
        TODO("Not yet implemented")
    }

    override fun registerToken(registerTokenRequest: dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest): dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse {
        val requestMetadata: Map<String, String?> =
            ObjectMapper().convertValue(registerTokenRequest.metadata ?: JsonNodeFactory.instance.objectNode())
                ?: emptyMap()
        val metadata = requestMetadata + mapOf("lzCustomerId" to registerTokenRequest.customerId)

        val params = CustomerCreateParams.builder()
            .setAddress(
                Address.builder()
                    .setCountry(registerTokenRequest.billingInformation?.address?.country)
                    .setLine1(registerTokenRequest.billingInformation?.address?.lineOne)
                    .setLine2(registerTokenRequest.billingInformation?.address?.lineTwo)
                    .setPostalCode(registerTokenRequest.billingInformation?.address?.postalCode)
                    .setState(registerTokenRequest.billingInformation?.address?.state)
                    .setCity(registerTokenRequest.billingInformation?.address?.city)
                    .build()
            )
            .setEmail(registerTokenRequest.billingInformation?.email)
            .setName(registerTokenRequest.billingInformation?.name)
            .setMetadata(metadata)
            .addExpand("sources")
            .build()

        val lzCustomerId = registerTokenRequest.customerId

        val customer: Customer = findCustomer(lzCustomerId) ?: Customer.create(params)

        val source = mapOf(
            "object" to "card",
            "number" to String(registerTokenRequest.creditCardNumber.accountNumber),
            "exp_month" to registerTokenRequest.expMonth,
            "exp_year" to registerTokenRequest.expYear,
            "cvc" to registerTokenRequest.cardVerificationValue,
            "name" to registerTokenRequest.billingInformation?.name,
            "address_line1" to registerTokenRequest.billingInformation?.address?.lineOne,
            "address_line2" to registerTokenRequest.billingInformation?.address?.lineTwo,
            "address_state" to registerTokenRequest.billingInformation?.address?.state,
            "address_zip" to registerTokenRequest.billingInformation?.address?.postalCode,
            "address_country" to registerTokenRequest.billingInformation?.address?.country,
            "address_city" to registerTokenRequest.billingInformation?.address?.city
        )
        val cardParams = mapOf(
            "source" to source,
            "metadata" to metadata
        )

        //Try to create Card using the customer
        val card: Card =
            catchingCardException("Unable to create Stripe card method", registerTokenRequest.gatewayRequestId) {
                customer.sources.create(
                    cardParams,
                    RequestOptions
                        .getDefault()
                        .toBuilder()
                        .setIdempotencyKey(registerTokenRequest.gatewayRequestId)
                        .build()
                ) as Card
            }
        logger.info("Created Stripe card method, gateway request id ${registerTokenRequest.gatewayRequestId}")

        val cardId = dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse(
            token = dev.odmd.platform.springcdk.gateways.StripeToken(card.id, customer.id).toToken(),
            responseMessage = card.status ?: "",
            responseCode = card.status ?: "",
            gatewayTransactionIdentifier = card.id,
        )

        return (cardId)
    }

    override fun decodeToken(token: String): dev.odmd.platform.springcdk.gateways.GatewayToken {
        return dev.odmd.platform.springcdk.gateways.StripeToken.fromToken(token)
    }

    private fun findCustomer(lzCustomerId: String): Customer? {
        val customerSearchParams = CustomerSearchParams.builder()
            .setQuery("metadata['lzCustomerId']:'${lzCustomerId}'")
            .addExpand("data.sources")
            .build()

        val customerSearchResult = Customer.search(customerSearchParams)
        // If there is more than one customer per LzCustomerId note the error
        if (customerSearchResult.data.size > 1) {
            logger.error("Found more than one Stripe customer with customerId: ${lzCustomerId}")
        }
        // If there is exactly one customer set the customer value to the existing customer
        return customerSearchResult.data.firstOrNull()
    }

    fun authGatewayBuilder(
        request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest,
        charge: Charge,
        status: TransactionStatus,
        token: String
    ): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {

        val chargeAmount = scaledLongToMonetaryAmount(charge.currency.uppercase(Locale.getDefault()), charge.amount)
        return dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
            request = request.toString(),
            response = charge.toString(),
            gatewayIdentifier = this.gatewayIdentifier,
            status = status,
            transactionDatetime = Instant.now(),
            amountAuthorized = chargeAmount,
            declineCode = null,
            declineReason = null,
            token = token

        )
    }

    fun <T> catchingCardException(message: String, gatewayRequestId: GatewayRequestId, create: () -> T): T {
        try {
            return create()
        } catch (cardException: CardException) {
            /**
             * Stripe's Java SDK throws CardException on 402 response:
             * https://github.com/stripe/stripe-java/blob/ecb791f567f85377f60b272eb9f7a4d91bfb67b7/src/main/java/com/stripe/net/LiveStripeResponseGetter.java#L203-L213
             */
            logger.event(
                "stripe.payment-declined", mapOf(
                    "message" to message,
                    "gatewayRequestId" to gatewayRequestId,
                    "exceptionMessage" to cardException.message
                ), Level.INFO
            )
            throw dev.odmd.platform.springcdk.gateways.GatewayRequestFailureException(
                gatewayRequestId = gatewayRequestId,
                message = message,
                cause = cardException
            )
        }
    }
}

private const val STRIPE_SECRET_KEY_PREFIX = "sk_"
private const val STRIPE_SECRET_KEY_PREFIX_TEST_ENV = "sk_test"

@ConfigurationProperties(prefix = "app.gateway.stripe")
@Component
class StripeConfiguration {
    private val logger = LoggerFactory.getLogger(StripeConfiguration::class.java)
    lateinit var apiKey: String

    val isApiKeyInitialized
        get() = this::apiKey.isInitialized

    fun validateApiKey() {
        val prefix = apiKey.take(3)
        if (!apiKey.startsWith(STRIPE_SECRET_KEY_PREFIX)) {
            logger.error("Stripe API key must start with $STRIPE_SECRET_KEY_PREFIX, got $prefix")
            throw Exception("Stripe API key must start with '$STRIPE_SECRET_KEY_PREFIX', got $prefix")
        } else {
            if (apiKey.startsWith(STRIPE_SECRET_KEY_PREFIX_TEST_ENV)) {
                logger.info("Using Stripe's test environment")
            } else {
                logger.info("Using Stripe's live environment")
            }
        }
    }
}
