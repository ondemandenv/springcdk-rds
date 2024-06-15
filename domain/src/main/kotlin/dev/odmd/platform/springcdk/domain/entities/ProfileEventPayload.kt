package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.common.ProfilePaymentType
import dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted
import java.time.Instant

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME
)
sealed interface DomainEventPayload

data class ProfileActivatedEvent(
    val profileId: String,
    val customerId: String,
    val gateway: String,
    val paymentType: ProfilePaymentType,
    val isReusable: Boolean,
    val profileCreatedAt: Instant,
    @dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted val paymentMethod: dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethod
) : dev.odmd.platform.springcdk.domain.entities.DomainEventPayload
{
    enum class PaymentMethodType {
        SAVED_CREDIT_CARD
    }

    data class PaymentMethod(
        val type: dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodType,
        val savedCreditCard: dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.SavedCreditCard?
    ) {
        constructor(savedCreditCardPaymentMethod: dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.SavedCreditCard) : this(
            type = dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodType.SAVED_CREDIT_CARD,
            savedCreditCard = savedCreditCardPaymentMethod
        )

        fun methodInformation(): dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodInformation = when (type) {
                dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodType.SAVED_CREDIT_CARD -> savedCreditCard as dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodInformation
            }
    }

    sealed interface PaymentMethodInformation

    data class SavedCreditCard(
        val firstDigit: Int,
        val lastFourDigits: String,
        val expMonth: Int,
        val expYear: Int,
        val billingInformation: BillingInformation? = null
    ) : dev.odmd.platform.springcdk.domain.entities.ProfileActivatedEvent.PaymentMethodInformation {
        val creditCardNetwork = CreditCardNetwork.fromFirstDigit(firstDigit)
    }
}

data class ProfileDeactivatedEvent(
    val profileId: String
) : dev.odmd.platform.springcdk.domain.entities.DomainEventPayload

data class ProfileUpdatedEvent(
    val profileId: String,
    @dev.odmd.platform.springcdk.domain.cryptography.JsonEncrypted val billingInformation: BillingInformation?,
    // expiration
    // card network
    // last four
) : dev.odmd.platform.springcdk.domain.entities.DomainEventPayload
