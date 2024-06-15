package dev.odmd.platform.springcdk.gateways

import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.gateways.worldpay.WorldPayClient
import dev.odmd.platform.springcdk.gateways.worldpay.WorldPayConfiguration
import dev.odmd.platform.springcdk.gateways.worldpay.conversions.*
import io.github.vantiv.sdk.generate.*
import org.javamoney.moneta.FastMoney
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant

@Service
internal class WorldPayGatewayService(
    val config: WorldPayConfiguration,
    private val worldPayClient: WorldPayClient
) : PaymentGateway() {
    companion object {
        internal val logger = LoggerFactory.getLogger(WorldPayGatewayService::class.java)
    }

    override fun authorize(request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        MDC.putCloseable("gatewayRequestId", request.gatewayRequestId).use {
            logger.event(
                "worldpay.requestStarted", mapOf(
                    "requestType" to "authorize"
                )
            )

            return if (request.shouldCapture) {
                val worldPaySaleRequest = when (request) {
                    is dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest -> request.toWorldPaySaleRequest()
                    is dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest -> request.toWorldPaySaleRequest()
                }

                val response = worldPayClient.sale(worldPaySaleRequest)

                logger.event(
                    "worldpay.requestEnded", mapOf(
                        "worldPayRequestType" to "sale",
                        "worldPayResponseCode" to response.response,
                        "worldPayResponseMessage" to response.message,
                        "worldPayCnpTxnId" to response.cnpTxnId
                    )
                )
                convertSaleToAuthorizeResponse(
                    request = request,
                    response = response,
                    worldPaySaleRequest
                )
            } else {
                val worldPayAuthorizeRequest = when (request) {
                    is dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest -> request.toWorldPayAuthorizeRequest()
                    is dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest -> request.toWorldPayAuthorizeRequest()
                }

                val response = worldPayClient.authorize(worldPayAuthorizeRequest)

                logger.event(
                    "worldpay.requestEnded", mapOf(
                        "worldPayRequestType" to "authorize",
                        "worldPayResponseCode" to response.response,
                        "worldPayResponseMessage" to response.message,
                        "worldPayCnpTxnId" to response.cnpTxnId
                    )
                )

                convertAuthorizeResponse(
                    request = request,
                    response = response,
                    worldPayAuthorizeRequest
                )
            }
        }
    }

    override fun capture(captureRequest: dev.odmd.platform.springcdk.gateways.GatewayCaptureRequest): dev.odmd.platform.springcdk.gateways.GatewayCaptureResponse {
        TODO("Not yet implemented")
    }

    override fun refund(refundRequest: dev.odmd.platform.springcdk.gateways.GatewayRefundRequest): dev.odmd.platform.springcdk.gateways.GatewayRefundResponse {
        val creditRequest = refundRequest.toWorldPayCreditRequest()
        val creditResponse = worldPayClient.credit(creditRequest)

        return convertCreditToRefundResponse(refundRequest, creditResponse, creditRequest)
    }

    private fun convertCreditToRefundResponse(
        request: dev.odmd.platform.springcdk.gateways.GatewayRefundRequest,
        response: CreditResponse,
        worldPayCreditRequest: Credit
    ): dev.odmd.platform.springcdk.gateways.GatewayRefundResponse {
        val worldPayCreditResponse = response.toWorldPayCreditResponse()

        return if (config.transactionSuccessCodes.contains(response.response)) {
            dev.odmd.platform.springcdk.gateways.GatewayRefundResponse(
                refundType = request.refundType,
                refundAmount = request.refundAmount,
                request = worldPayCreditRequest,
                response = worldPayCreditResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.SUCCESS,
                transactionDatetime = Instant.now(),
                declineCode = null,
                declineReason = null
            )
        } else {
            dev.odmd.platform.springcdk.gateways.GatewayRefundResponse(
                refundType = request.refundType,
                refundAmount = request.refundAmount,
                request = worldPayCreditRequest,
                response = worldPayCreditResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.DECLINED,
                transactionDatetime = Instant.now(),
                declineCode = response.response,
                declineReason = response.message
            )
        }
    }

    override fun registerToken(registerTokenRequest: dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest): dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse {
        MDC.putCloseable("gatewayRequestId", registerTokenRequest.gatewayRequestId).use {
            logger.event(
                "worldpay.requestStarted", mapOf(
                    "requestType" to "registerToken"
                )
            )

            val response = worldPayClient.registerToken(registerTokenRequest.toWorldPayRequest())

            logger.event(
                "worldpay.requestEnded", mapOf(
                    "worldPayResponseCode" to response.response,
                    "worldPayResponseMessage" to response.message,
                    "worldPayCnpTxnId" to response.cnpTxnId
                )
            )

            return convertRegisterTokenResponse(
                gatewayRequestId = registerTokenRequest.gatewayRequestId,
                response = response
            )
        }
    }

    override fun decodeToken(token: String): dev.odmd.platform.springcdk.gateways.GatewayToken {
        return dev.odmd.platform.springcdk.gateways.WorldPayToken(token)
    }

    private fun convertRegisterTokenResponse(
        gatewayRequestId: GatewayRequestId,
        response: RegisterTokenResponse
    ): dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse {
        if (config.registerTokenSuccessCodes.contains(response.response)) {
            if (response.cnpToken.isNullOrBlank()) {
                throw EmptyTokenError(
                    gatewayRequestId = gatewayRequestId,
                    worldpayTransactionId = response.cnpTxnId
                ).also { logger.warn(it.message) }
            }
            return dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse(
                token = response.cnpToken,
                responseMessage = response.message,
                responseCode = response.response,
                gatewayTransactionIdentifier = response.cnpTxnId.toString()
            )
        } else {
            throw WorldPayError(
                gatewayRequestId = gatewayRequestId,
                worldpayTransactionId = response.cnpTxnId,
                responseCode = response.response,
                responseMessage = response.message
            )
        }
    }

    private fun convertAuthorizeResponse(
        request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest,
        response: AuthorizationResponse,
        authorizeRequest: Authorization
    ): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        val worldPayAuthorizeRequest = authorizeRequest.toWorldPayAuthorizeRequest()
        val worldPayAuthorizeResponse = response.toWorldPayAuthorizationResponse()
        return if (config.transactionSuccessCodes.contains(response.response)) {
            dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
                request = worldPayAuthorizeRequest,
                response = worldPayAuthorizeResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.SUCCESS,
                transactionDatetime = Instant.now(),
                amountAuthorized = request.authorizeAmount,
                declineCode = null,
                declineReason = null,
                token = response.tokenResponse?.cnpToken
            )
        } else {
            dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
                request = worldPayAuthorizeRequest,
                response = worldPayAuthorizeResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.DECLINED,
                transactionDatetime = Instant.now(),
                amountAuthorized = FastMoney.of(BigDecimal.ZERO, request.authorizeAmount.currency),
                declineCode = response.response,
                declineReason = response.message,
                token = response.tokenResponse?.cnpToken
            )
        }
    }

    private fun convertSaleToAuthorizeResponse(
        request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest,
        response: SaleResponse,
        worldPayRequest: Sale
    ): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        val worldPaySaleRequest = worldPayRequest.toWorldPaySaleRequest()
        val worldPaySaleResponse = response.toWorldPaySaleResponse()

        return if (config.transactionSuccessCodes.contains(response.response)) {
            return dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
                request = worldPaySaleRequest,
                response = worldPaySaleResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.SUCCESS,
                transactionDatetime = Instant.now(),
                amountAuthorized = request.authorizeAmount,
                declineCode = null,
                declineReason = null,
                token = response.tokenResponse?.cnpToken
            )
        } else {
            dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
                request = worldPaySaleRequest,
                response = worldPaySaleResponse,
                gatewayIdentifier = response.cnpTxnId.toString(),
                status = TransactionStatus.DECLINED,
                transactionDatetime = Instant.now(),
                amountAuthorized = FastMoney.of(BigDecimal.ZERO, request.authorizeAmount.currency),
                declineCode = response.response,
                declineReason = response.message,
                token = response.tokenResponse?.cnpToken
            )
        }
    }

}

class WorldPayError(
    gatewayRequestId: GatewayRequestId,
    val worldpayTransactionId: Long,
    val responseCode: String,
    val responseMessage: String
) : dev.odmd.platform.springcdk.gateways.GatewayRequestFailureException(
    gatewayRequestId = gatewayRequestId,
    message = "WorldPay request with cnpTxnId=$worldpayTransactionId failed: $responseCode - $responseMessage."
)

class EmptyTokenError(
    gatewayRequestId: GatewayRequestId,
    val worldpayTransactionId: Long
) : dev.odmd.platform.springcdk.gateways.GatewayRequestFailureException(
    gatewayRequestId = gatewayRequestId,
    message = "WorldPay transaction $worldpayTransactionId contained a null or empty token."
)

