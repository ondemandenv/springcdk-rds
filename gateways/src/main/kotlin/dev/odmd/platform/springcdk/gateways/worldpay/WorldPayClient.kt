package dev.odmd.platform.springcdk.gateways.worldpay

import io.github.vantiv.sdk.generate.*

interface WorldPayClient {
    fun registerToken(tokenRequest: RegisterTokenRequestType): RegisterTokenResponse
    fun sale(toWorldPaySaleRequest: Sale): SaleResponse
    fun authorize(toWorldPayAuthorizeRequest: Authorization): AuthorizationResponse
    fun credit(credit: Credit): CreditResponse
}
