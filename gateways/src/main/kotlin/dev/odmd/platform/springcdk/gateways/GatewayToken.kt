package dev.odmd.platform.springcdk.gateways

sealed interface GatewayToken {
    val gatewayIdentifier: String
}

data class WorldPayToken(val token: String) : dev.odmd.platform.springcdk.gateways.GatewayToken {
    override val gatewayIdentifier: String = dev.odmd.platform.springcdk.gateways.Gateways.WORLDPAY.name
}

data class StripeToken(
    val paymentMethodId: String,
    val stripeCustomerId: String
) : dev.odmd.platform.springcdk.gateways.GatewayToken {
    companion object {
        private const val UNICODE_SEPARATOR = '\u001D'

        fun fromToken(token: String): dev.odmd.platform.springcdk.gateways.StripeToken {
            val split = token.split(dev.odmd.platform.springcdk.gateways.StripeToken.Companion.UNICODE_SEPARATOR)
            if (split.size != 2) {
                throw IllegalArgumentException("Unexpected format for Stripe token starting with ${token.take(4)}")
            }
            return dev.odmd.platform.springcdk.gateways.StripeToken(split[1], split[0])
        }
    }

    override val gatewayIdentifier: String = dev.odmd.platform.springcdk.gateways.Gateways.STRIPE.name

    fun toToken(): String = toString()

    override fun toString(): String {
        return "${stripeCustomerId}${dev.odmd.platform.springcdk.gateways.StripeToken.Companion.UNICODE_SEPARATOR}${paymentMethodId}"
    }
}

data class NoopToken(val token: String) : dev.odmd.platform.springcdk.gateways.GatewayToken {
    override val gatewayIdentifier: String = dev.odmd.platform.springcdk.gateways.Gateways.NOOP.name
}
