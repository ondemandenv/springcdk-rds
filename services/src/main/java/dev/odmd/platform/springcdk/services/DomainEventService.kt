package dev.odmd.platform.springcdk.services

import dev.odmd.platform.springcdk.common.EventName
import dev.odmd.platform.springcdk.domain.entities.*
import dev.odmd.platform.springcdk.domain.repositories.DomainEventRepository
import org.springframework.stereotype.Service

@Service
class DomainEventService(
    private val domainEventRepository: DomainEventRepository
) {
    fun didActivateProfile(paymentProfile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile) {
        val paymentMethod = when (val paymentMethodInformation = paymentProfile.methodInformation) {
            is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethod(
                dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodType.SAVED_CREDIT_CARD,
                dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.SavedCreditCard(
                    paymentMethodInformation.creditCardInfo.firstDigit,
                    paymentMethodInformation.creditCardInfo.lastFourDigits,
                    paymentMethodInformation.creditCardInfo.expMonth,
                    paymentMethodInformation.creditCardInfo.expYear,
                    paymentMethodInformation.billingInformation
                )
            )
        }

        DomainEvent(
            eventName = EventName.PROFILE_ACTIVATED.eventName,
            aggregateId = paymentProfile.externalId,
            customerId = paymentProfile.customerId,
            payload = dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent(
                profileId = paymentProfile.externalId,
                customerId = paymentProfile.customerId,
                gateway = paymentProfile.gatewayIdentifier,
                paymentType = paymentProfile.profilePaymentType,
                isReusable = paymentProfile.isReusable,
                profileCreatedAt = paymentProfile.createdDateTime,
                paymentMethod = paymentMethod
            )
        ).let {
            domainEventRepository.save(it)
        }
    }

    fun didUpdateProfile(paymentProfile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile) {
        val domainEvent = DomainEvent(
            eventName = EventName.PROFILE_UPDATED.eventName,
            aggregateId = paymentProfile.externalId,
            customerId = paymentProfile.customerId,
            payload = dev.odmd.platform.springcdk.domain.entities.ProfileUpdatedEvent(
                profileId = paymentProfile.externalId,
                billingInformation = null
            )
        ).let {
            domainEventRepository.save(it)
        }
    }

    fun didDeactivateProfile(paymentProfileId: String, customerId: String) {
        val domainEvent = DomainEvent(
            eventName = EventName.PROFILE_DEACTIVATED.eventName,
            aggregateId = paymentProfileId,
            customerId = customerId,
            payload = dev.odmd.platform.springcdk.domain.entities.ProfileDeactivatedEvent(
                profileId = paymentProfileId
            )
        ).let {
            domainEventRepository.save(it)
        }
    }
}