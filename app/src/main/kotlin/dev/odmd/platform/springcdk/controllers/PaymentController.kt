package dev.odmd.platform.springcdk.controllers

import dev.odmd.platform.springcdk.common.MockRequestResponse
import dev.odmd.platform.springcdk.common.PaymentPaging
import dev.odmd.platform.springcdk.model.v1.*
import dev.odmd.platform.springcdk.security.SpelExpressions.Companion.FIRST_PAYMENT_PROFILE_ID
import dev.odmd.platform.springcdk.security.SpelExpressions.Companion.PAYMENT_PROFILE_IDS
import dev.odmd.platform.springcdk.services.PaymentProfileService
import dev.odmd.platform.springcdk.services.PaymentService
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import javax.money.Monetary
import javax.validation.Valid

@RestController
class PaymentController(
    val paymentService: PaymentService,
    val profileService: PaymentProfileService
) : dev.odmd.platform.springcdk.api.v1.PaymentHttpService {

    /**
     * todo: these validations should be moved into service?
     */
    @PreAuthorize("@paymentProfileAuthorization.ownsAll(${PAYMENT_PROFILE_IDS})")
    override fun createPaymentAuthorization(
        lzEntityId: String,
        paymentGatewayId: String?,
        authorizePaymentRequest: AuthorizePaymentRequest,
        expectedRequest: String?,
        expectedResponse: String?
    ): AuthorizePaymentResponse {

        if (authorizePaymentRequest.paymentDtos.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one payment needed")
        }

        if( authorizePaymentRequest.paymentDtos.size != 1 ){
            throw ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Only support one payment for now" )
        }

        if (authorizePaymentRequest.lineItemDtos.size != authorizePaymentRequest.lineItemDtos.groupBy { it.id }.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "lineItem should have unique id")
        }

        if (authorizePaymentRequest.paymentDtos.size != authorizePaymentRequest.paymentDtos.groupBy { it.paymentMethod }.size) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment should have unique payment method")
        }

        if (!Monetary.isCurrencyAvailable(authorizePaymentRequest.currencyCode)) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST, "unknown currency code: ${authorizePaymentRequest.currencyCode}"
            )
        }

        val mockedRequestResponse = MockRequestResponse.fromHeaders(expectedRequest, expectedResponse)

        // This is for backwards compatibility with Billing API $0 auth calls.
        val paymentMethod = paymentService.getZeroDollarAuthPaymentMethod(authorizePaymentRequest.paymentDtos)
        if (paymentMethod != null) {
            return profileService.validate(
                requestId = authorizePaymentRequest.requestId,
                targetKey = authorizePaymentRequest.targetKey,
                targetType = authorizePaymentRequest.targetType,
                customerId = lzEntityId,
                creditCardPaymentMethod = paymentMethod,
                currencyCode = authorizePaymentRequest.currencyCode,
                paymentMetadata = authorizePaymentRequest.paymentDtos.firstOrNull()?.paymentMetadata,
                gatewayIdentifier = paymentGatewayId,
                mockedRequestResponse = mockedRequestResponse
            )
        }

        return paymentService.authorizePayment(authorizePaymentRequest, paymentGatewayId, lzEntityId, mockedRequestResponse)
    }

   @PostAuthorize("@paymentProfileAuthorization.isOwner(${FIRST_PAYMENT_PROFILE_ID})")
   override fun listPayments(targetType: String, targetKey: String): List<PaymentResponse> {
        if (targetType.isEmpty())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Target type cannot be empty")
        if (targetKey.isEmpty())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Target key cannot be empty")
        return paymentService.getAllPaymentsForTarget(targetType, targetKey)
    }

    @PreAuthorize("@paymentAuthorization.isOwner(#paymentId)")
    override fun getPaymentById(paymentId: String): PaymentResponse {
        if (paymentId.isEmpty())
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment Id cannot be empty")


        return paymentService.getSinglePayment(paymentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    override fun stopPayment(paymentId: String, stopPaymentRequest: StopPaymentRequest): StopPaymentResponse {
        TODO("Not yet implemented")
    }

    @PreAuthorize("@paymentAuthorization.isOwner(#paymentId)")
    override fun capturePayment(
        paymentId: String, capturePaymentRequest: CapturePaymentRequest
    ): CapturePaymentResponse {
        TODO("Not yet implemented")
    }

    override fun cancelPayment(paymentId: String, @Valid cancelPaymentRequest: CancelPaymentRequest): CancelPaymentResponse {
        TODO("Not yet implemented")
    }

    override fun getPaymentRefundOptions(targetType: String, targetKey: String): GetPaymentRefundOptionsResponse {
        TODO("Not yet implemented")
    }

    @PreAuthorize("@paymentAuthorization.isOwner(#paymentId)")
    override fun refundPayment(
        paymentId: String,
        refundPaymentRequest: RefundPaymentRequest,
        lzEntityId: String,
        expectedRequest: String?,
        expectedResponse: String?
    ): RefundPaymentResponse {
        return paymentService.refundPayment(paymentId, refundPaymentRequest, lzEntityId, MockRequestResponse.fromHeaders(expectedRequest, expectedResponse))
    }

    @PreAuthorize("@paymentGatewayTransactionAuthorization.isOwner(#recordRefundRequest.gatewayIdentifier)")
    override fun recordRefundPayment(
        recordRefundRequest: RecordRefundRequest,
        lzEntityId: String
    ): ResponseEntity<Unit> {
        return paymentService.recordRefundPayment(recordRefundRequest, lzEntityId)
    }

    @PreAuthorize("@userOwnerAuthorization.isOwner(#customerId)")
    override fun getPaymentsByCustomerId(customerId: String, page: Int?, size: Int?): Page<PaymentResponse> {
        if (size != null && size > PaymentPaging.maxPageSize)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Size should be less than ${PaymentPaging.maxPageSize}")

        return paymentService.getPaymentsByCustomerId(customerId, page ?: PaymentPaging.defaultPage, size ?: PaymentPaging.defaultSize)
    }
}

