package dev.odmd.platform.springcdk.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.odmd.platform.springcdk.common.MockRequestResponse
import dev.odmd.platform.springcdk.common.ProfilePaymentType
import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.domain.IdempotentRequestExecutor
import dev.odmd.platform.springcdk.domain.entities.PaymentProfile
import dev.odmd.platform.springcdk.domain.repositories.PaymentProfileRepository
import dev.odmd.platform.springcdk.gateways.PaymentGatewaySupplier
import dev.odmd.platform.springcdk.model.v1.*
import dev.odmd.platform.springcdk.services.errors.InvalidMethodInformationUpdate
import dev.odmd.platform.springcdk.services.utils.mapper.toGetProfileDto
import dev.odmd.platform.springcdk.services.utils.mapper.toPaymentProfileDto
import org.javamoney.moneta.FastMoney
import org.jetbrains.kotlin.konan.file.use
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.util.*
import javax.money.Monetary
import javax.transaction.Transactional

@Service
@EnableConfigurationProperties(dev.odmd.platform.springcdk.domain.config.DomainEventConfig::class)
class PaymentProfileService(
    val paymentGatewayFactory: PaymentGatewaySupplier,
    val paymentProfileRepository: PaymentProfileRepository,
    val idempotentRequestExecutor: IdempotentRequestExecutor,
    val transitContext: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService,
    val domainEventService: DomainEventService,
    val domainEventConfig: dev.odmd.platform.springcdk.domain.config.DomainEventConfig
) {
    companion object {
        val logger = LoggerFactory.getLogger(PaymentProfileService::class.java)
    }

    fun create(
        customerId: String,
        requestId: RequestId,
        reusable: Boolean,
        default: Boolean,
        creditCardPaymentMethod: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod,
        gatewayIdentifier: String? = null,
        mockedRequestResponse: MockRequestResponse? = null,
        metadata: JsonNode? = null,
        activeProfile: Boolean
    ): dev.odmd.platform.springcdk.domain.entities.PaymentProfile {
        return idempotentRequestExecutor.performOnce(
            requestId = requestId,
            alreadyPerformed = { profileId ->
                paymentProfileRepository.findById(profileId).orElseThrow {
                    IllegalStateException("Expected profile $profileId to have already been created by request $requestId but was not found.")
                }
            }
        ) {
            val gateway = if (gatewayIdentifier != null) {
                paymentGatewayFactory.get(gatewayIdentifier, mockedRequestResponse)
            } else {
                paymentGatewayFactory.default()
            }

            val externalId = UUID.randomUUID().toString()

            creditCardPaymentMethod.creditCardNumber.use {
                val tokenResponse = gateway.registerToken(
                    dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest(
                        requestId,
                        creditCardPaymentMethod.creditCardNumber,
                        creditCardPaymentMethod.cardVerificationValue,
                        creditCardPaymentMethod.expMonth,
                        creditCardPaymentMethod.expYear,
                        customerId,
                        creditCardPaymentMethod.billingInformation,
                        JsonNodeFactory.instance.pojoNode(mapOf("lzPaymentProfileId" to externalId))
                    )
                )

                val creditCardPaymentMethodInformation =
                    dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation(
                        creditCardPaymentMethod.billingInformation,
                        dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation.CreditCardInfo(
                            tokenResponse.token,
                            creditCardPaymentMethod.creditCardNumber.firstDigit,
                            creditCardPaymentMethod.creditCardNumber.lastFour,
                            creditCardPaymentMethod.expMonth,
                            creditCardPaymentMethod.expYear
                        )
                    )
                dev.odmd.platform.springcdk.domain.entities.PaymentProfile(
                    externalId,
                    default,
                    reusable,
                    activeProfile,
                    customerId,
                    creditCardPaymentMethodInformation,
                    gateway.gatewayIdentifier,
                    ProfilePaymentType.CREDIT_CARD,
                    metadata
                ).let { profile ->
                    if (activeProfile) {
                        activateProfile(profile)
                    } else {
                        paymentProfileRepository.save(profile)
                    }
                    (profile.id to profile)
                }
            }
        }
    }

    /**
     * Get or create [PaymentProfile] entities using each payment's [PaymentMethod].
     *
     * Throws an exception if any profiles cannot be found or created.
     */
    fun getOrCreateProfilesForPayments(
        customerId: String,
        paymentDtos: List<dev.odmd.platform.springcdk.model.v1.PaymentDto>,
        gatewayIdentifier: String? = null,
        activeProfile: Boolean,
        mockRequestResponse: MockRequestResponse? = null
    ): List<Pair<dev.odmd.platform.springcdk.model.v1.PaymentDto, dev.odmd.platform.springcdk.domain.entities.PaymentProfile>> {
        val savedProfileExternalIds = paymentDtos
            .mapNotNull {
                when (val method = it.paymentMethod.unwrap()) {
                    is dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod -> method
                    is dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod -> null
                }
            }
            .map { it.paymentProfileId }
        val savedProfiles = paymentProfileRepository.findByExternalIdInAndIsActive(savedProfileExternalIds.toSet(), activeProfile)

        return paymentDtos.map { paymentDto ->
            val profile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile = when (val paymentMethod = paymentDto.paymentMethod.unwrap()) {
                is dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod -> {
                    val profileExternalId = paymentMethod.paymentProfileId

                    val email = paymentDto.paymentMethod.savedCreditCard?.email

                    val profile = savedProfiles.single { it.externalId == profileExternalId }

                    if (!email.isNullOrEmpty()) {
                        updateBillingEmail(email, profile)
                    }

                    profile
                }

                is dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod -> {
                    val requestId = UUID.randomUUID().toString()

                    logger.info("Creating profile with requestId $requestId")

                    create(
                        customerId = customerId,
                        // TODO: shouldn't use higher-level API which might cause issues w/ idempotency
                        requestId = requestId,
                        // TODO: get reusable/default from request
                        false,
                        false,
                        dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod(
                            paymentMethod.creditCardNumber,
                            paymentMethod.expMonth,
                            paymentMethod.expYear,
                            paymentMethod.cardVerificationValue,
                            paymentMethod.billingInformation
                        ),
                        gatewayIdentifier = gatewayIdentifier,
                        mockedRequestResponse = mockRequestResponse,
                        activeProfile = activeProfile
                    )
                }
            }
            paymentDto to profile
        }
    }

    fun getByCustomerId(customerId: String): List<dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto> =
        paymentProfileRepository.findByCustomerIdAndIsActiveTrue(customerId).map { it.toGetProfileDto() }

    @Transactional
    fun stitchProfile(customerId: String, guestSessionCustomerId: String): Int {
        transitContext.scopedContext = guestSessionCustomerId.encodeToByteArray()
        val paymentProfiles = paymentProfileRepository.findByCustomerId(guestSessionCustomerId)
        paymentProfiles.forEach { it.customerId = customerId }
        val count = paymentProfiles.size

        transitContext.scopedContext = customerId.encodeToByteArray()
        paymentProfileRepository.saveAll(paymentProfiles)

        logger.info("$count payment profiles have been updated from $guestSessionCustomerId to $customerId")
        return count
    }

    fun getByExternalId(
        customerId: String,
        externalId: String
    ): dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto? {
        return paymentProfileRepository.findByExternalIdAndIsActiveTrue(externalId)?.toGetProfileDto()
    }

    fun deleteByExternalId(externalId: String): Boolean {
        val p = paymentProfileRepository.findByExternalIdAndIsActiveTrue(externalId)
        p?.let {
            it.isActive = false
            paymentProfileRepository.save(it)
            return true
        }
        return false
    }

    fun updateProfile(
        externalId: String,
        update: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.Update
    ): PaymentProfileDto? =
        paymentProfileRepository.findByExternalIdAndIsActiveTrue(externalId)?.let { profile ->
            when (update) {
                is dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.CreditCardUpdate -> updateCreditCardProfile(
                    update,
                    profile
                )
            }
            paymentProfileRepository.save(profile).toPaymentProfileDto()
        }

    fun validate(
        requestId: String,
        targetKey: String,
        targetType: String,
        customerId: String,
        creditCardPaymentMethod: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod,
        currencyCode: String,
        paymentMetadata: JsonNode?,
        gatewayIdentifier: String?,
        mockedRequestResponse: MockRequestResponse? = null
    ): AuthorizePaymentResponse {
        val gateway = if (gatewayIdentifier != null) {
            paymentGatewayFactory.get(gatewayIdentifier, mockedRequestResponse)
        } else {
            paymentGatewayFactory.default()
        }

        val gatewayAuthorizeRequest = dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest(
            customerId = customerId,
            gatewayRequestId = UUID.randomUUID().toString(),
            authorizeAmount = FastMoney.zero(Monetary.getCurrency(currencyCode)),
            creditCardNumber = creditCardPaymentMethod.creditCardNumber,
            expMonth = creditCardPaymentMethod.expMonth,
            expYear = creditCardPaymentMethod.expYear,
            billingInformation = creditCardPaymentMethod.billingInformation,
            metadata = paymentMetadata,
            shouldCapture = false,
            billingDescriptor = "",
            targetKey = null
        )
        val gatewayAuthorizeResponse = gateway.authorize(gatewayAuthorizeRequest)
        val errorResponse = if (gatewayAuthorizeResponse.status != TransactionStatus.SUCCESS)
            mutableListOf(
                ErrorResponse(
                    gatewayAuthorizeResponse.declineCode?.toIntOrNull() ?: -1,
                    gatewayAuthorizeResponse.declineReason ?: ""
                )
            )
        else emptyList()
        return AuthorizePaymentResponse(
            targetKey,
            requestId,
            targetType,
            errorResponse,
            emptyList(),
            emptyList()
        )
    }

    fun getToken(externalId: String): dev.odmd.platform.springcdk.gateways.GatewayToken? {
        val profile = paymentProfileRepository.findByExternalIdAndIsActiveTrue(externalId) ?: return null

        return when (val methodInformation = profile.methodInformation) {
            is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation ->
                paymentGatewayFactory.get(profile.gatewayIdentifier)
                    .decodeToken(methodInformation.creditCardInfo.gatewayToken)

            else -> null
        }
    }

    private fun updateCreditCardProfile(
        update: dev.odmd.platform.springcdk.model.v1.UpdatePaymentProfileRequest.CreditCardUpdate,
        profile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile
    ) {
        val methodInformation = profile.methodInformation
        if (methodInformation !is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation) {
            throw InvalidMethodInformationUpdate("creditCard", profile.externalId)
        }

        profile.methodInformation = methodInformation.copy(
            creditCardInfo = methodInformation.creditCardInfo.copy(
                expMonth = update.expirationMonth,
                expYear = update.expirationYear
            ),
            billingInformation = update.billingInformation
        )
    }

    private fun updateBillingEmail(
        email: String,
        profile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile
    ) {
        when (val methodInformation = profile.methodInformation) {
            is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> {
                profile.methodInformation = methodInformation.copy(
                    billingInformation = methodInformation.billingInformation?.copy(
                        email = email
                    )
                )
            }
        }
        paymentProfileRepository.save(profile)
    }

    @Transactional(rollbackOn = [Exception::class])
    fun activateProfile(profile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile) {
        profile.isActive = true
        paymentProfileRepository.save(profile)

        if (domainEventConfig.enabled) {
            domainEventService.didActivateProfile(profile)
        }
    }
}
