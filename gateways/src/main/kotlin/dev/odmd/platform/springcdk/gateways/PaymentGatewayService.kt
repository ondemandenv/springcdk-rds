package dev.odmd.platform.springcdk.gateways

/**
 * Public interface exposed to other modules.
 *
 * Within the gateways module, implementing classes should inherit from [PaymentGateway] instead.
 */
interface PaymentGatewayService {

    fun authorize(request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse

    fun capture(captureRequest: dev.odmd.platform.springcdk.gateways.GatewayCaptureRequest): dev.odmd.platform.springcdk.gateways.GatewayCaptureResponse

    fun refund(refundRequest: dev.odmd.platform.springcdk.gateways.GatewayRefundRequest): dev.odmd.platform.springcdk.gateways.GatewayRefundResponse

    fun registerToken(registerTokenRequest: dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest): dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse

    fun decodeToken(token: String): dev.odmd.platform.springcdk.gateways.GatewayToken

    /**
     * Unique identifier for a specific gateway.
     *
     * An opaque string is exposed to other modules, but see [Gateways] for the list
     * of supported gateways and how to obtain their specific [PaymentGatewayService] implementation.
     */
    val gatewayIdentifier: String
}
