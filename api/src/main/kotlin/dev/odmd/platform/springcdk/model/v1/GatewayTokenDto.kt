package dev.odmd.platform.springcdk.model.v1

sealed interface GatewayTokenDto

data class StripeTokenDto(val paymentMethodId: String, val stripeCustomerId: String) : GatewayTokenDto

data class SimpleToken(val token: String) : GatewayTokenDto
