package dev.odmd.platform.springcdk.gateways.worldpay

import io.github.vantiv.sdk.CnpOnline
import io.github.vantiv.sdk.generate.*
import org.springframework.stereotype.Service

@Service
internal class CnpOnlineWrapper(private val config: WorldPayConfiguration) : WorldPayClient {
    val properties by lazy { config.worldpayClientProperties }

    /**
     * WorldPay client is not thread-safe, so we create a new one per request.
     *
     * This was discovered by running e2e tests in parallel (the default when using gradle),
     * which failed due to an IllegalStateException that was thrown by the WorldPay client
     * attempting to reallocate an existing connection.
     */
    val client: CnpOnline
        get() = CnpOnline(properties)

    override fun registerToken(tokenRequest: RegisterTokenRequestType): RegisterTokenResponse {
        return client.registerToken(tokenRequest)
    }

    override fun sale(toWorldPaySaleRequest: Sale): SaleResponse {
        return client.sale(toWorldPaySaleRequest)
    }

    override fun authorize(toWorldPayAuthorizeRequest: Authorization): AuthorizationResponse {
        return client.authorize(toWorldPayAuthorizeRequest)
    }

    override fun credit(credit: Credit): CreditResponse {
        return client.credit(credit)
    }
}
