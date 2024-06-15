package dev.odmd.platform.springcdk.gateways.worldpay.conversions

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.*
import dev.odmd.platform.springcdk.gateways.*
import io.github.vantiv.sdk.generate.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * This file contains functions that are used to convert payments-platform data into
 * WorldPay (aka CnpOnline/Vantiv) data.
 */

internal fun CreditCardNetwork.toMethodOfPaymentTypeEnum(): MethodOfPaymentTypeEnum =
    when (this) {
        CreditCardNetwork.AMEX -> MethodOfPaymentTypeEnum.AX
        CreditCardNetwork.VISA -> MethodOfPaymentTypeEnum.VI
        CreditCardNetwork.MASTERCARD -> MethodOfPaymentTypeEnum.MC
        CreditCardNetwork.DISCOVER -> MethodOfPaymentTypeEnum.DI
        CreditCardNetwork.UNKNOWN -> MethodOfPaymentTypeEnum.BLANK
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest.toWorldPayCustomBilling() =
    CustomBilling().apply {
        descriptor = Constants.billingDescriptorPrefix + billingDescriptor
        phone = Constants.billingDescriptorPhone
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest.toWorldPayRequest() =
    RegisterTokenRequestType().also { worldpayRequest ->
        worldpayRequest.id = gatewayRequestId
        worldpayRequest.accountNumber = String(creditCardNumber.accountNumber)
        worldpayRequest.cardValidationNum = cardVerificationValue ?: ""
        worldpayRequest.customerId = customerId
        worldpayRequest.reportGroup = metadata?.get("reportGroup")?.asText()
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest.toWorldPayAuthorizeRequest() =
    Authorization().also { auth ->
        auth.id = gatewayRequestId
        auth.customerId = customerId
        auth.orderId = targetKey
        auth.amount = authorizeAmount.toScaledLong()
        auth.orderSource = orderSourceFromMetadata(metadata)
        auth.reportGroup = metadata?.get("reportGroup")?.textValue()

        auth.token = CardTokenType().apply {
            cnpToken = token
            expDate = ExpirationFormat.twoDigitMonthAndYear(
                year = expYear,
                month = expMonth
            )
        }

        auth.billToAddress = billingInformation?.toWorldPayContact()

        auth.customBilling = CustomBilling().apply {
            descriptor = Constants.billingDescriptorPrefix + billingDescriptor
            phone = Constants.billingDescriptorPhone
        }

        auth.enhancedData = EnhancedData().apply {
            customerReference = targetKey?.take(WorldPayFieldLimits.customerReference)
        }
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest.toWorldPayAuthorizeRequest() =
    Authorization().also { auth ->
        auth.id = gatewayRequestId
        auth.customerId = ""
        auth.orderId = targetKey
        auth.amount = authorizeAmount.toScaledLong()

        auth.card = worldPayCardType()
        auth.reportGroup = metadata?.get("reportGroup")?.textValue()
        auth.orderSource = orderSourceFromMetadata(metadata)

        auth.billToAddress = billingInformation?.toWorldPayContact()

        auth.customBilling = toWorldPayCustomBilling()
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest.worldPayCardType(): CardType =
    CardType().also { card ->
        card.number = String(creditCardNumber.accountNumber)
        card.cardValidationNum = ""
        card.expDate = ExpirationFormat.twoDigitMonthAndYear(year = expYear, month = expMonth)
        card.type =  creditCardNumber.creditCardNetwork.toMethodOfPaymentTypeEnum()
    }

/*
    worldpay asked to not send billing information if the credit card network is MasterCard
 */
internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest.getWorldPayContactBillingInformation(): Contact? {
    return billingInformation?.toWorldPayContact()
        ?.takeIf { creditCardNetwork != CreditCardNetwork.MASTERCARD }
}

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest.toWorldPaySaleRequest() =
    Sale().also { sale ->
        sale.id = gatewayRequestId
        sale.customerId = customerId
        sale.amount = authorizeAmount.toScaledLong()

        sale.card = worldPayCardType()

        sale.orderSource = orderSourceFromMetadata(metadata)

        sale.billToAddress = billingInformation?.toWorldPayContact()

        sale.customBilling = toWorldPayCustomBilling()

        sale.reportGroup = metadata?.get("reportGroup")?.textValue()
    }

internal fun orderSourceFromMetadata(metadata: JsonNode?) =
    orderSourceOrDefault(metadata?.get("orderSource")?.textValue())

/**
 * Convert an optional order source into its enum.
 *
 * @return The matching [OrderSourceType], or "ecommerce" if null or invalid.
 */
internal fun orderSourceOrDefault(optOrderSource: String?) =
    optOrderSource?.let { orderSource ->
        try {
            OrderSourceType.fromValue(orderSource)
        } catch (e: IllegalArgumentException) {
            WorldPayGatewayService.logger.warn("Unknown order source: $orderSource")
            null
        }
    } ?: OrderSourceType.ECOMMERCE

internal fun dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest.toWorldPaySaleRequest() =
    Sale().also { sale ->
        sale.id = gatewayRequestId
        sale.customerId = customerId
        sale.orderId = targetKey
        sale.amount = authorizeAmount.toScaledLong()
        sale.orderSource = orderSourceFromMetadata(metadata)

        sale.token = CardTokenType().apply {
            cnpToken = token
            expDate = ExpirationFormat.twoDigitMonthAndYear(
                year = expYear,
                month = expMonth
            )
        }

        sale.billToAddress = getWorldPayContactBillingInformation()

        sale.customBilling = CustomBilling().apply {
            descriptor = Constants.billingDescriptorPrefix + billingDescriptor
            phone = Constants.billingDescriptorPhone
        }

        sale.enhancedData = EnhancedData().apply {
            customerReference = targetKey?.take(WorldPayFieldLimits.customerReference)
        }

        sale.reportGroup = metadata?.get("reportGroup")?.textValue()
    }

internal fun dev.odmd.platform.springcdk.gateways.GatewayRefundRequest.toWorldPayCreditRequest() =
    Credit().also { credit ->
        credit.reportGroup = refundMetadata?.get("reportGroup")?.textValue()
        credit.cnpTxnId = gatewayIdentifierToRefund.toLong()
        credit.id = gatewayRequestId
        credit.amount = refundAmount.toScaledLong()
        credit.customerId = customerId
        credit.enhancedData = EnhancedData().also { enhancedData ->
            enhancedData.customerReference = targetKey.take(WorldPayFieldLimits.customerReference)
        }
    }

internal fun BillingInformation.toWorldPayContact() =
    Contact().also { contact ->
        contact.name = name
        contact.firstName = firstName
        contact.lastName = lastName
        contact.addressLine1 = address.lineOne.take(WorldPayFieldLimits.address)
        contact.addressLine2 = address.lineTwo.take(WorldPayFieldLimits.address)
        contact.city = address.city.take(WorldPayFieldLimits.address)
        contact.state = address.state
        contact.zip = address.postalCode
        contact.country =
            try {
                CountryTypeEnum.fromValue(address.country)
            }
            catch (e: IllegalArgumentException) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Billing country is not valid as per Payment Gateway"
                )
            }
        contact.email = email
        contact.phone = phoneNumber
    }

object WorldPayFieldLimits {
    const val address = 35
    const val customerReference = 17
}
