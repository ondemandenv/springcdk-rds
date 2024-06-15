package dev.odmd.platform.springcdk.gateways.worldpay.conversions

import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.common.ExpirationFormat
import dev.odmd.platform.springcdk.common.MaskingMode
import dev.odmd.platform.springcdk.common.mask
import dev.odmd.platform.springcdk.gateways.worldpay.dto.*
import io.github.vantiv.sdk.generate.*
import java.time.temporal.ChronoField

/**
 * This file contains functions that are used to convert WorldPay DTOs into
 * Payments Platform DTOs which can be safely stored in the database since all sensitive data has
 * been removed.
 */

internal fun Authorization.toWorldPayAuthorizeRequest() =
    WorldPayAuthorizeRequest(
        gatewayRequestId = id,
        customerId = customerId,
        orderId = orderId,
        amount = amount,
        orderSource = orderSource?.toString(),
        cnpToken = token?.cnpToken?.mask(MaskingMode.LAST_FOUR),
        tokenExpDate = token?.expDate,
        state = billToAddress?.state,
        city = billToAddress?.city,
        descriptor = customBilling?.descriptor,
        customerReference = enhancedData?.customerReference
    )

internal fun AuthorizationResponse.toWorldPayAuthorizationResponse() =
    WorldPayAuthorizationResponse(
        cnpTxnId = cnpTxnId,
        response = response,
        orderId = orderId,
        cardProductId = cardProductId,
        message = message,
        location = location,
        approvedAmount = approvedAmount,
        cnpToken = tokenResponse?.cnpToken?.mask(MaskingMode.LAST_FOUR),
        tokenMessage = tokenResponse?.tokenMessage,
        tokenResponseCode = tokenResponse?.tokenResponseCode,
        giftCardResponse = giftCardResponse?.toWorldPayGiftCardResponse(),
        accountUpdater = accountUpdater?.toWorldPayAccountUpdater(),
        cardSuffix = cardSuffix,
        paymentAccountReferenceNumber = paymentAccountReferenceNumber?.mask(MaskingMode.LAST_FOUR),
        checkoutId = checkoutId,
        bin = tokenResponse?.bin,
        authCode = authCode,
        avsResult = fraudResult?.avsResult
    )


internal fun Sale.toWorldPaySaleRequest() = WorldPaySaleRequest(
    gatewayRequestId = id,
    cnpTxnId = cnpTxnId,
    customerId = customerId,
    orderId = orderId,
    amount = amount,
    orderSource = orderSource?.toString(),
    cnpToken = token?.cnpToken?.mask(MaskingMode.LAST_FOUR),
    tokenExpDate = token?.expDate,
    state = billToAddress?.state,
    city = billToAddress?.city,
    descriptor = customBilling?.descriptor,
    customerReference = enhancedData?.customerReference
)

internal fun SaleResponse.toWorldPaySaleResponse() = WorldPaySaleResponse(
    cnpTxnId = cnpTxnId,
    response = response,
    orderId = orderId,
    cardProductId = cardProductId,
    message = message,
    location = location,
    approvedAmount = approvedAmount,
    cnpToken = tokenResponse?.cnpToken?.mask(MaskingMode.LAST_FOUR),
    tokenMessage = tokenResponse?.tokenMessage,
    giftCardResponse = giftCardResponse?.toWorldPayGiftCardResponse(),
    accountUpdater = accountUpdater?.toWorldPayAccountUpdater(),
    cardSuffix = cardSuffix,
    paymentAccountReferenceNumber = paymentAccountReferenceNumber?.mask(MaskingMode.LAST_FOUR),
    checkoutId = checkoutId,
    avsResult = fraudResult?.avsResult,
    authCode = authCode
)

internal fun CreditResponse.toWorldPayCreditResponse() = WorldPayCreditResponse(
    cnpTxnId = cnpTxnId,
    response = response,
    message = message,
    location = location,
    cnpToken = tokenResponse?.cnpToken?.mask(MaskingMode.LAST_FOUR),
    tokenMessage = tokenResponse?.tokenMessage,
    checkoutId = checkoutId
)


internal fun GiftCardResponse.toWorldPayGiftCardResponse() =
    WorldPayGiftCardResponse(
        sequenceNumber = sequenceNumber?.mask(MaskingMode.LAST_FOUR),
        availableBalance = availableBalance,
        beginningBalance = beginningBalance,
        endingBalance = endingBalance,
        cashBackAmount = cashBackAmount
    )

internal fun AccountUpdater.toWorldPayAccountUpdater() =
    WorldPayAccountUpdater(
        originalCardInfo = originalCardInfo?.toWorldPayCardAccountInfo(),
        newCardInfo = newCardInfo?.toWorldPayCardAccountInfo(),
        extendedCardMessage = extendedCardResponse?.toExtendedCardMessage(),
        originalAccountInfo = originalAccountInfo?.toWorldPayEcheckAccountInfo(),
        newAccountInfo = newAccountInfo?.toWorldPayEcheckAccountInfo(),
        originalCardTokenInfo = originalCardTokenInfo?.toWorldPayTokenInfo(),
        newCardTokenInfo =  newCardTokenInfo?.toWorldPayTokenInfo()
    )


internal fun CardTokenInfoType.toWorldPayTokenInfo(): WorldPayTokenInfo {
    val expDateAccessor = ExpirationFormat.twoDigitMonthYearFormatter.parse(expDate)
    val firstDigit = bin.take(1).toInt()
    return WorldPayTokenInfo(
        token = cnpToken.mask(MaskingMode.LAST_FOUR),
        expMonth = expDateAccessor.get(ChronoField.MONTH_OF_YEAR),
        expYear = expDateAccessor.get(ChronoField.YEAR) % 100,
        firstDigit = firstDigit,
        type = CreditCardNetwork.fromFirstDigit(firstDigit)
    )
}

internal fun ExtendedCardResponseType.toExtendedCardMessage() =
    WorldPayExtendedMessage(message = message, code = code)
internal fun CardAccountInfoType.toWorldPayCardAccountInfo() =
    WorldPayCardAccountInfo(number = number?.mask(MaskingMode.LAST_FOUR), expDate = number)

internal fun EcheckAccountInfoType.toWorldPayEcheckAccountInfo() =
    WorldPayEcheckAccountInfo(
        accNum = accNum?.mask(MaskingMode.LAST_FOUR),
        routingNum = routingNum?.mask(MaskingMode.LAST_FOUR),
        accountType = accType.name
    )

