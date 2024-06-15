package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import dev.odmd.platform.springcdk.common.*
import dev.odmd.platform.springcdk.gateways.DeclineCode
import dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse
import dev.odmd.platform.springcdk.gateways.GatewayResponse
import dev.odmd.platform.springcdk.gateways.Gateways
import com.litle.sdk.generate.Contact
import java.time.temporal.ChronoField

internal val numericRegex = Regex("\\d+")

private const val approvedResponseMessage = "Approved"
private const val genericDeclineResponseCode: DeclineCode = "350"
private const val transactionApprovedResponseCode = "000"
private const val registerTokenApprovedCode = "801"

internal fun decodeTokenGateway(token: String): Gateways {
    return with(token) {
        when {
            contains("_") -> Gateways.STRIPE
            matches(numericRegex) -> Gateways.WORLDPAY
            else -> throw ProxiedRequestUnsupportedOperationException(
                "Unable to find correct gateway for authorization with given token with first four digits ${
                    token.mask(MaskingMode.FIRST_FOUR)
                }"
            )
        }
    }
}

internal fun decodeTransactionIdGateway(transactionId: String): Gateways {
    return with(transactionId) {
        when {
            contains("_") -> Gateways.STRIPE
            matches(numericRegex) -> Gateways.WORLDPAY
            else -> throw ProxiedRequestUnsupportedOperationException("Unable to find correct gateway for authorization with transaction id $transactionId")
        }
    }
}

internal fun decodeExpDate(expDate: String?) = if (expDate != null) {
    val expDateAccessor = ExpirationFormat.twoDigitMonthYearFormatter.parse(expDate)
    Pair(
        expDateAccessor.get(ChronoField.MONTH_OF_YEAR),
        expDateAccessor.get(ChronoField.YEAR) % 100
    )
} else {
    throw ProxiedRequestMissingParametersException("expDate")
}

internal fun mapGatewayResponseToWorldPayResponseCode(authorizationResponse: dev.odmd.platform.springcdk.gateways.GatewayResponse): String =
    if (authorizationResponse.status == TransactionStatus.SUCCESS) {
        transactionApprovedResponseCode
    } else if (authorizationResponse.declineCode?.matches(numericRegex) == true) {
        authorizationResponse.declineCode
    } else {
        genericDeclineResponseCode
    }

internal fun mapGatewayResponseToWorldPayMessage(authorizationResponse: dev.odmd.platform.springcdk.gateways.GatewayResponse): String? =
    if (authorizationResponse.status == TransactionStatus.SUCCESS) {
        approvedResponseMessage
    } else {
        authorizationResponse.declineReason
    }

internal fun mapGatewayRegisterTokenResponseToWorldPayResponse(registerTokenResponse: dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenResponse): String {
    return if (registerTokenResponse.responseCode.matches(numericRegex)) {
        registerTokenResponse.responseCode
    } else {
        registerTokenApprovedCode
    }
}

internal fun mapContactToBillingInformation(billToNode: Contact): BillingInformation {
    val name = billToNode.name ?: ""
    val nameSplit = name.split(" ")
    return BillingInformation(
        name = name,
        firstName = nameSplit.first(),
        lastName = nameSplit.last(),
        email = billToNode.email ?: "",
        phoneNumber = billToNode.phone ?: "",
        address = dev.odmd.platform.springcdk.common.Address(
            billToNode.country?.name ?: "",
            billToNode.addressLine1 ?: "",
            billToNode.addressLine2 ?: "",
            billToNode.zip ?: "",
            billToNode.state ?: "",
            billToNode.city ?: ""
        )
    )
}
