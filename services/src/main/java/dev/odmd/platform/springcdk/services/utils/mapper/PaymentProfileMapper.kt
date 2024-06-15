package dev.odmd.platform.springcdk.services.utils.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation
import dev.odmd.platform.springcdk.domain.entities.PaymentProfile
import dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto
import dev.odmd.platform.springcdk.model.v1.PaymentMethod
import dev.odmd.platform.springcdk.model.v1.PaymentProfileDto
import dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod

fun dev.odmd.platform.springcdk.domain.entities.PaymentProfile.toGetProfileDto(): dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto {
    val paymentMethod: dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethod =
        when (val methodInformation = methodInformation) {
            is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.PaymentMethod(
                dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto.SavedCreditCard(
                    paymentProfileId = externalId,
                    lastFourDigits = methodInformation.creditCardInfo.lastFourDigits,
                    expirationMonth = methodInformation.creditCardInfo.expMonth,
                    expirationYear = methodInformation.creditCardInfo.expYear,
                    type = CreditCardNetwork.fromFirstDigit(methodInformation.creditCardInfo.firstDigit),
                    email = methodInformation.billingInformation?.email.orEmpty(),
                    billingInformation = methodInformation.billingInformation
                )
            )
        }
    return dev.odmd.platform.springcdk.model.v1.GetPaymentProfileDto(
        id = externalId,
        customerId = customerId,
        reusable = isReusable,
        default = isDefault,
        paymentMethod = paymentMethod,
        metadata = metadata
    )
}

fun dev.odmd.platform.springcdk.domain.entities.PaymentProfile.toPaymentProfileDto(): PaymentProfileDto {
    val paymentMethod = when (val methodInformation = methodInformation) {
        is dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation -> PaymentMethod(
            dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod(
                paymentProfileId = externalId,
                lastFourDigits = methodInformation.creditCardInfo.lastFourDigits,
                expirationMonth = methodInformation.creditCardInfo.expMonth,
                expirationYear = methodInformation.creditCardInfo.expYear,
                type = CreditCardNetwork.fromFirstDigit(methodInformation.creditCardInfo.firstDigit),
                email = methodInformation.billingInformation?.email.orEmpty()
            )
        )
    }
    return PaymentProfileDto(
        id = externalId,
        customerId = customerId,
        reusable = isReusable,
        default = isDefault,
        paymentMethod = paymentMethod,
        metadata = metadataWithGateway(gatewayIdentifier, metadata)
    )
}

fun metadataWithGateway(gateway: String, metadata: JsonNode?): JsonNode {
    val newNode = if (metadata == null) {
        JsonNodeFactory.instance.objectNode()
    } else {
        metadata.deepCopy()
    }
    return newNode.put("_gateway", gateway)
}
