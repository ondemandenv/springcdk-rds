package dev.odmd.platform.springcdk.gateways

import dev.odmd.platform.springcdk.common.TransactionStatus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
internal class NoopLoggingGatewayService constructor(private val logger: Logger) : PaymentGateway() {
    constructor() : this(
        LoggerFactory.getLogger(NoopLoggingGatewayService::class.java)
    )

    override fun authorize(request: dev.odmd.platform.springcdk.gateways.GatewayAuthorizeRequest): dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse {
        val token = when (request) {
            is dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest -> {
                logger.info(
                    "Credit card authorization request: {} with card ending in {}",
                    request.authorizeAmount,
                    request.creditCardNumber.lastFour
                )
                "authorize token"
            }
            is dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest -> {
                logger.info(
                    "Credit card token authorization request: {} with token {}", request.authorizeAmount, request.token
                )
                request.token
            }
        }

        return dev.odmd.platform.springcdk.gateways.GatewayAuthorizeResponse(
            request = "{}",
            response = "{}",
            gatewayIdentifier = this.gatewayIdentifier,
            status = TransactionStatus.SUCCESS,
            transactionDatetime = Instant.now(),
            amountAuthorized = request.authorizeAmount,
            declineCode = null,
            declineReason = null,
            token = token
        )
    }

    override fun capture(captureRequest: dev.odmd.platform.springcdk.gateways.GatewayCaptureRequest): dev.odmd.platform.springcdk.gateways.GatewayCaptureResponse {
        logger.info(
            "Capture request: {} with previous authorization token {}",
            captureRequest.captureAmount,
            captureRequest.authorizationReference
        )
        return dev.odmd.platform.springcdk.gateways.GatewayCaptureResponse(
            captureRequest.gatewayRequestId,
            "capture response",
            gatewayIdentifier,
            TransactionStatus.SUCCESS,
            Instant.now(),
            "decline-code",
            "decline-reason",
            captureRequest.authorizationReference
        )
    }

    override fun refund(refundRequest: dev.odmd.platform.springcdk.gateways.GatewayRefundRequest): dev.odmd.platform.springcdk.gateways.GatewayRefundResponse {
        logger.info("Refund request: {} via method {}", refundRequest.refundAmount, refundRequest.refundType.type)
        return dev.odmd.platform.springcdk.gateways.GatewayRefundResponse(
            refundRequest.refundType,
            refundRequest.refundAmount,
            "",
            "",
            refundRequest.gatewayRequestId,
            TransactionStatus.SUCCESS,
            Instant.now(),
            null,
            null
        )
    }

    override fun registerToken(
        registerTokenRequest: dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest
    ): dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse {
        logger.info("Register request: {}", registerTokenRequest.creditCardNumber.lastFour)
        val token = UUID.randomUUID().toString()
        return dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse(
            token = token, responseCode = "200", responseMessage = "Success", gatewayTransactionIdentifier = token
        )
    }

    override fun decodeToken(token: String): dev.odmd.platform.springcdk.gateways.GatewayToken {
        return dev.odmd.platform.springcdk.gateways.NoopToken(token)
    }
}
