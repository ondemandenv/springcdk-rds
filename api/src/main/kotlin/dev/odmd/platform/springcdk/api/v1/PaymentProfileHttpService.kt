package dev.odmd.platform.springcdk.api.v1

import dev.odmd.platform.springcdk.common.RequestHeaders
import dev.odmd.platform.springcdk.model.v1.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.NotBlank

@Tag(name = "Payment Profiles", description = "Payment Profile Management")
@RequestMapping("payment-profiles")
interface PaymentProfileHttpService {
    @Operation(
        summary = "Create Payment Profile",
        description = "\${payment-profile-service.payment-method-explanation}"
    )

    @PostMapping("")
    fun createPaymentProfile(
        @Parameter(description = "\${payment-profile-service.customer-id-explanation}", hidden = true)
        @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
        @Parameter(description = "\${payment-profile-service.payment-gateway-id-explanation}")
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_ID) paymentGatewayId: String?,
        @Valid @RequestBody request: CreatePaymentProfileRequest,

        /**
         * expectedRequest
         *
         * The WorldPay API request payload which will be used during verification.
         *
         */
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_REQUEST, required = false) expectedRequest: String?,

        /**
         * expectedResponse
         *
         * The WorldPay API response payload which will be returned by the mock gateway client.
         *
         */
        @Parameter(hidden = true)
        @RequestHeader(RequestHeaders.ODMD_GATEWAY_RESPONSE, required = false) expectedResponse: String?,
    ): PaymentProfileDto

    @Operation(summary = "update a guest payment profile with a user payment profile")
    @PostMapping("stitch")
    fun stitchTempProfileWithUser(
        @Valid @NotBlank @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
        @Valid @RequestBody request: StitchProfileRequest
    ): ResponseEntity<Unit>

    @Operation(summary = "Get Payment Profiles for Customer")
    @GetMapping
    fun getPaymentProfiles(
        @Parameter(description = "\${payment-profile-service.customer-id-explanation}", hidden = true)
        @Valid @NotBlank @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String
    ): List<GetPaymentProfileDto>

    @Operation(summary = "Get Payment Profile by Id")
    @GetMapping("{profileId}")
    fun getPaymentProfile(
        @Parameter(description = "\${payment-profile-service.profile-id-explanation}", hidden = true)
        @Valid @NotBlank @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
        @PathVariable profileId: String
    ): GetPaymentProfileDto

    @Operation(summary = "Update Payment Profile")
    @PutMapping("{profileId}")
    fun updatePaymentProfile(
        @Parameter(description = "\${payment-profile-service.profile-id-explanation}")
        @PathVariable profileId: String,
        @RequestBody request: UpdatePaymentProfileRequest
    ): PaymentProfileDto

    @Operation(summary = "Delete Payment Profile")
    @DeleteMapping("{profileId}")
    fun deletePaymentProfile(
        @Parameter(description = "\${payment-profile-service.profile-id-explanation}")
        @PathVariable profileId: String
    ): ResponseEntity<Unit>

    @Operation(summary = "Get Profile Token")
    @GetMapping("{profileId}/token")
    fun getProfileToken(
        @Parameter(description = "\${payment-profile-service.profile-id-explanation}")
        @Valid @NotBlank @PathVariable profileId: String,
        @Parameter(description = "\${payment-profile-service.customer-id-explanation}", hidden = true)
        @RequestHeader(RequestHeaders.ODMD_ENTITY_ID) lzEntityId: String,
    ): GatewayTokenDto
}
