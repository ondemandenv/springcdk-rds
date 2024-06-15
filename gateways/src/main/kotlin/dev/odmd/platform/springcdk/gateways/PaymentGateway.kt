package dev.odmd.platform.springcdk.gateways

/**
 * Internal base class for all [PaymentGatewayService] implementations.
 *
 * This class provides the logic for associating [PaymentGatewayService] implementations with one
 * of the known [Gateways].
 */

internal sealed class PaymentGateway : PaymentGatewayService {
    val gateway: Gateways
        get() = when (this) {
            is NoopLoggingGatewayService -> Gateways.NOOP
            is WorldPayGatewayService -> Gateways.WORLDPAY
            is StripeGatewayService -> Gateways.STRIPE
        }

    override val gatewayIdentifier: String
        get() = gateway.name
}

