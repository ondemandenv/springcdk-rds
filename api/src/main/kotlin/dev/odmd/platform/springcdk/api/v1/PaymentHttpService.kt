package dev.odmd.platform.springcdk.api.v1

import dev.odmd.platform.springcdk.common.RequestHeaders
import dev.odmd.platform.springcdk.model.v1.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Tag(name = "Payments", description = "Payment Management")
@RequestMapping("payments")
interface PaymentHttpService {
    @PostMapping("authorize")
    // This is needed to avoid null result in PaymentsControllerSecurityTest
    @ResponseBody
    @Operation(
        summary = "Create Payment Authorization",
        description = "\${payment-service.create-payment-authorization}"
    )
    fun createPaymentAuthorization(
        @Parameter(description = "\${payment-profile-service.customer-id-explanation}", hidden = true)
        @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
        @Parameter(description = "\${payment-profile-service.payment-gateway-id-explanation}")
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_ID) paymentGatewayId: String?,
        @Valid @RequestBody authorizePaymentRequest: dev.odmd.platform.springcdk.model.v1.AuthorizePaymentRequest,
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_REQUEST, required = false) expectedRequest: String?,
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_RESPONSE, required = false) expectedResponse: String?,
    ): dev.odmd.platform.springcdk.model.v1.AuthorizePaymentResponse

    @GetMapping
    // This is needed to avoid circular view handling errors when there are no payments for the target type & key.
    @ResponseBody
    @Operation(summary = "List Payments for Target", description = "\${payment-service.list-payments}")
    fun listPayments(
        @Parameter(description = "\${payment-service.target-type-explanation}")
        @RequestParam targetType: String,
        @Parameter(description = "\${payment-service.target-key-explanation}")
        @RequestParam targetKey: String
    ): List<PaymentResponse>

    @GetMapping("{paymentId}")
    @Operation(summary = "Get Payment by Id", description = "\${payment-service.get-payment-by-id}")
    fun getPaymentById(@PathVariable paymentId: String): PaymentResponse

    @PutMapping("{paymentId}/stop")
    @Operation(summary = "Stop Payment", description = "\${payment-service.stop-payment}")
    fun stopPayment(
        @PathVariable paymentId: String,
        @RequestBody stopPaymentRequest: StopPaymentRequest
    ): StopPaymentResponse

    @PutMapping("{paymentId}/capture")
    @Operation(summary = "Capture Payment", description = "\${payment-service.capture-payment}")
    fun capturePayment(
        @PathVariable paymentId: String,
        @RequestBody capturePaymentRequest: CapturePaymentRequest
    ): CapturePaymentResponse

    @PutMapping("{paymentId}/cancel")
    @Operation(summary = "Cancel Payment", description = "\${payment-service.cancel-payment}")
    fun cancelPayment(
        @PathVariable paymentId: String,
        @RequestBody @Valid cancelPaymentRequest: CancelPaymentRequest
    ): CancelPaymentResponse

    @GetMapping("refund-options")
    @Operation(summary = "Get Payment Refund Options", description = "\${payment-service.payment-refund-options}")
    fun getPaymentRefundOptions(
        @Parameter(description = "\${payment-service.target-type-explanation}")
        @RequestParam targetType: String,
        @Parameter(description = "\${payment-service.target-key-explanation}")
        @RequestParam targetKey: String
    ): GetPaymentRefundOptionsResponse

    @PutMapping("{paymentId}/refund")
    @Operation(summary = "Refund Payment", description = "\${payment-service.payment-refund}")
    fun refundPayment(
        @PathVariable paymentId: String,
        @RequestBody refundPaymentRequest: RefundPaymentRequest,
        @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_REQUEST, required = false) expectedRequest: String?,
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_RESPONSE, required = false) expectedResponse: String?,
    ): RefundPaymentResponse


    @PostMapping("record-refund")
    @Operation(summary = "To record a refund from the Verfi chargeback system", description = "\${payment-service.payment-refund}")
    fun recordRefundPayment(
        @Valid  @RequestBody  recordRefundRequest: RecordRefundRequest,
        @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
    ): ResponseEntity<Unit>

    @GetMapping("search")
    @ResponseBody
    @Operation(summary = "List Payments for customerId", description = "search\${customerId}")
    fun getPaymentsByCustomerId(
        @Parameter(description = "\${payment-service.customerId}")
        @Valid @NotBlank @RequestParam customerId: String,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): Page<PaymentResponse>
}
