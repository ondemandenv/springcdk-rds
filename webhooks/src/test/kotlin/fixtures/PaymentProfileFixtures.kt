import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.Address
import com.ondemand.platform.payments.common.BillingInformation
import com.ondemand.platform.payments.common.ProfilePaymentType
import dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation
import dev.odmd.platform.springcdk.domain.entities.PaymentMethodInformation
import dev.odmd.platform.springcdk.domain.entities.PaymentProfile
import kotlin.random.Random

fun getPaymentProfile(
    externalId: String = "externalId",
    isDefault: Boolean = true,
    isReusable: Boolean = true,
    isActive: Boolean = true,
    customerId: String = "customerId",
    paymentMethodInformation: dev.odmd.platform.springcdk.domain.entities.PaymentMethodInformation = getPaymentMethodInformation(),
    profilePaymentType: ProfilePaymentType = ProfilePaymentType.CREDIT_CARD,
    gatewayIdentifier: String = "gatewayId",
    metadata: JsonNode? = null
) =
    dev.odmd.platform.springcdk.domain.entities.PaymentProfile(
        externalId = externalId,
        isDefault = isDefault,
        isReusable = isReusable,
        isActive = isActive,
        customerId = customerId,
        methodInformation = paymentMethodInformation,
        profilePaymentType = profilePaymentType,
        gatewayIdentifier = gatewayIdentifier,
        metadata = metadata
    ).apply {
        id = Random.nextLong()
    }

fun getPaymentMethodInformation(
    billingInformation: BillingInformation = getBillingInformation(),
    creditCardInformation: dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation.CreditCardInfo = getCreditCardInfo()
) = dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation(
    billingInformation,
    creditCardInformation
)

fun getCreditCardInfo(
    gatewayToken: String = "card_1234",
    firstDigit: Int = 1,
    lastFourDigits: String = "2342",
    expMonth: Int = 9,
    expYear: Int = 2099,
) = dev.odmd.platform.springcdk.domain.entities.CreditCardPaymentMethodInformation.CreditCardInfo(
    gatewayToken = gatewayToken,
    firstDigit = firstDigit,
    lastFourDigits = lastFourDigits,
    expMonth = expMonth,
    expYear = expYear,
)

fun getBillingInformation(
    name: String = "name",
    firstName: String = "firstName",
    lastName: String = "lastName",
    email: String = "email@email.com",
    phoneNumber: String = "3432212433",
    address: dev.odmd.platform.springcdk.common.Address = dev.odmd.platform.springcdk.common.Address(
        "USA",
        "lineOne",
        "lineTwo",
        "10012",
        "NY",
        "NY",
    ),
) = BillingInformation(
    name = name,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phoneNumber = phoneNumber,
    address = address,
)

