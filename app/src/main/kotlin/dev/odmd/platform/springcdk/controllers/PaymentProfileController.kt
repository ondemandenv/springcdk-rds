package dev.odmd.platform.springcdk.controllers

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.odmd.platform.springcdk.api.v1.PaymentProfileHttpService
import dev.odmd.platform.springcdk.common.CurrencyAmount
import dev.odmd.platform.springcdk.common.MockRequestResponse
import dev.odmd.platform.springcdk.common.ProfileValidation
import dev.odmd.platform.springcdk.common.creditCardNetwork
import dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation
import dev.odmd.platform.springcdk.exceptions.ProfileTokenNotFound
import dev.odmd.platform.springcdk.gateways.GatewayToken
import dev.odmd.platform.springcdk.gateways.NoopToken
import dev.odmd.platform.springcdk.gateways.StripeToken
import dev.odmd.platform.springcdk.gateways.WorldPayToken
import dev.odmd.platform.springcdk.model.v1.*
import dev.odmd.platform.springcdk.services.PaymentProfileService
import dev.odmd.platform.springcdk.services.PaymentService
import dev.odmd.platform.springcdk.services.utils.mapper.toPaymentProfileDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
class PaymentProfileController(
    val paymentProfileService: PaymentProfileService,
    val paymentService: PaymentService
) : dev.odmd.platform.springcdk.api.v1.PaymentProfileHttpService {
    override fun createPaymentProfile(
        lzEntityId: String,
        paymentGatewayId: String?,
        request: dev.odmd.platform.springcdk.model.v1.CreatePaymentProfileRequest,
        expectedRequest: String?,
        expectedResponse: String?
    ): PaymentProfileDto {
        val creditCardPaymentMethod =
            request.method.creditCard
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only ${dev.odmd.platform.springcdk.model.v1.PaymentType.CREDIT_CARD} payment method type is supported"
                )

        val mockedRequestResponse = MockRequestResponse.fromHeaders(expectedRequest, expectedResponse)

        val paymentProfile = paymentProfileService.create(
            lzEntityId,
            request.requestId,
            request.reusable,
            request.default,
            creditCardPaymentMethod,
            gatewayIdentifier = paymentGatewayId,
            mockedRequestResponse,
            metadata = request.metadata,
            activeProfile = (!request.validate)
        )

        if (!request.validate) {
            return paymentProfile.toPaymentProfileDto()
        }

        val paymentMethod = when(val methodInformation = paymentProfile.methodInformation) {
            is CreditCardPaymentMethodInformation -> {
                PaymentMethod(
                    dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod(
                        paymentProfile.externalId,
                        methodInformation.creditCardInfo.lastFourDigits,
                        methodInformation.creditCardInfo.expMonth,
                        methodInformation.creditCardInfo.expYear,
                        methodInformation.creditCardInfo.creditCardNetwork,
                        methodInformation.billingInformation?.email
                    )
                )

            }
        }

        val authorizePaymentRequest = dev.odmd.platform.springcdk.model.v1.AuthorizePaymentRequest(
            requestId = request.requestId,
            currencyCode = ProfileValidation.defaultCurrencyCode,
            targetType = ProfileValidation.paymentTargetType,
            targetKey = lzEntityId,
            reason = ProfileValidation.transactionReason,
            source = ProfileValidation.transactionSource,
            billingDescriptor = "",
            lineItemDtos = listOf(),
            paymentDtos = listOf(
                dev.odmd.platform.springcdk.model.v1.PaymentDto(
                    paymentMethod = paymentMethod,
                    lineItemIds = listOf(),
                    shouldCapture = false,
                    authorizeAmount = CurrencyAmount("0.00"),
                    paymentMetadata = request.metadata ?: JsonNodeFactory.instance.objectNode()
                )
            )
        )

        val authorizePaymentResponse = paymentService.authorizePayment(
            request = authorizePaymentRequest,
            gatewayIdentifier = paymentGatewayId,
            customerId = lzEntityId,
            mockedRequestResponse = mockedRequestResponse,
            requireActiveProfile = false
        )

        if(authorizePaymentResponse.errors?.isNotEmpty() == true) {
            throw ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "${authorizePaymentResponse.errors}")
        }

        paymentProfileService.activateProfile(paymentProfile)

        return authorizePaymentResponse.paymentProfiles.first()
    }

    override fun stitchTempProfileWithUser(lzEntityId: String, request: StitchProfileRequest): ResponseEntity<Unit> {
        paymentProfileService.stitchProfile(lzEntityId, request.guestSessionCustomerId)
        return ResponseEntity(HttpStatus.OK)
    }

    override fun getPaymentProfiles(lzEntityId: String): List<GetPaymentProfileDto> {
        return paymentProfileService.getByCustomerId(lzEntityId)
    }

    @PreAuthorize("@paymentProfileAuthorization.isOwner(#profileId)")
    override fun getPaymentProfile(
        lzEntityId: String,
        profileId: String
    ): dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto {
        if (profileId.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile Id cannot be empty")
        }

        return paymentProfileService.getByExternalId(lzEntityId, profileId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PreAuthorize("@paymentProfileAuthorization.isOwner(#profileId)")
    override fun updatePaymentProfile(
        profileId: String,
        request: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest
    ): PaymentProfileDto {
        val update: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.Update = when (request.updateType) {
            dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.UpdateType.CREDIT_CARD -> request.creditCard
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Must specify updated credit card information when type is ${dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.UpdateType.CREDIT_CARD.name}."
                )
        }
        return paymentProfileService.updateProfile(profileId, update)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    @PreAuthorize("@paymentProfileAuthorization.isOwner(#profileId)")
    override fun deletePaymentProfile(profileId: String): ResponseEntity<Unit> {
        if (profileId.trim().isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile Id cannot be empty")
        }
        return if (paymentProfileService.deleteByExternalId(profileId)) {
            ResponseEntity(HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.NOT_FOUND)
        }
    }

    @PreAuthorize("@paymentProfileAuthorization.isOwner(#profileId)")
    override fun getProfileToken(profileId: String, lzEntityId: String): GatewayTokenDto {
        return paymentProfileService.getToken(profileId)?.toDto() ?: throw ProfileTokenNotFound(profileId)
    }
}

fun dev.odmd.platform.springcdk.gateways.GatewayToken.toDto(): GatewayTokenDto {
    return when (this) {
        is dev.odmd.platform.springcdk.gateways.StripeToken -> StripeTokenDto(this.paymentMethodId, this.stripeCustomerId)
        is dev.odmd.platform.springcdk.gateways.WorldPayToken -> SimpleToken(this.token)
        is dev.odmd.platform.springcdk.gateways.NoopToken -> SimpleToken(this.token)
    }
}
