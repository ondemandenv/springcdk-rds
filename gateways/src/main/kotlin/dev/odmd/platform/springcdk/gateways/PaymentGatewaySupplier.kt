package dev.odmd.platform.springcdk.gateways

import dev.odmd.platform.springcdk.common.MockRequestResponse
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.gateways.worldpay.CnpOnlineVerifier
import dev.odmd.platform.springcdk.gateways.worldpay.WorldPayConfiguration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
class PaymentGatewaySupplier @Autowired internal constructor(
    private val configuration: PaymentGatewayConfiguration,
    private val noopLoggingGateway: NoopLoggingGatewayService,
    private val stripeGatewayService: StripeGatewayService,
    private val worldPayGatewayService: WorldPayGatewayService,
    private val worldPayConfiguration: WorldPayConfiguration
) {
    companion object {
        val logger = LoggerFactory.getLogger(PaymentGatewaySupplier::class.java)
    }

    fun default(): PaymentGatewayService = get(configuration.default)

    internal fun get(identifier: Gateways): PaymentGatewayService =
        when (identifier) {
            Gateways.NOOP -> noopLoggingGateway
            Gateways.WORLDPAY -> worldPayGatewayService
            Gateways.STRIPE -> stripeGatewayService
        }

    internal fun getMock(
        identifier: Gateways,
        mockRequestResponse: MockRequestResponse
    ): PaymentGatewayService =
        when (identifier) {
            Gateways.WORLDPAY -> WorldPayGatewayService(
                worldPayConfiguration,
                CnpOnlineVerifier(
                    worldPayConfiguration = worldPayConfiguration,
                    mockRequestResponse = mockRequestResponse
                )
            )
            else -> throw NotImplementedError("mock version $identifier has not been implemented")
        }

    fun get(identifier: String, mockRequestResponse: MockRequestResponse? = null): PaymentGatewayService {
        val gateway = try {
            Gateways.valueOf(identifier)
        } catch (e: IllegalArgumentException) {
            throw UnknownGatewayError(identifier)
        }
        return if (mockRequestResponse != null) {
            logger.event("Using mocked gateway", mapOf("gatewayIdentifier" to gateway))
            getMock(identifier = gateway, mockRequestResponse = mockRequestResponse)
        } else {
            get(gateway)
        }
    }
}

data class UnknownGatewayError(val unknownGatewayId: String) :
    Exception("Cannot create gateway with unrecognized identifier: $unknownGatewayId")


@ConfigurationProperties(prefix = "app.gateway")
@Component
internal class PaymentGatewayConfiguration {
    lateinit var default: Gateways
    var proxyDefault: Gateways = Gateways.WORLDPAY
}
