package fixtures

import com.ondemand.platform.payments.common.TransactionStatus
import com.ondemand.platform.payments.common.TransactionType
import com.ondemand.platform.payments.domain.entities.*
import dev.odmd.platform.springcdk.webhooks.STRIPE_PAYMENT_TARGET_TYPE
import getPaymentProfile
import java.math.BigDecimal
import java.time.Instant
import kotlin.random.Random

internal fun getPayment(
    paymentTarget: dev.odmd.platform.springcdk.domain.entities.PaymentTarget,
    externalPaymentId: String = "externalPaymentId",
    paymentDateTime: Instant = Instant.parse("2022-06-14T20:25:57.607293Z"),
    stoppedDateTime: Instant? = null,
    requestedBy: String = "UnitTest",
    currency: String = "USD",
    currencyAmount: BigDecimal = "350.00".toBigDecimal(),
    paymentMetadata: dev.odmd.platform.springcdk.domain.entities.PaymentMetadata? = null,
    paymentTransactions: MutableList<PaymentTransaction>? = mutableListOf()
) =
    dev.odmd.platform.springcdk.domain.entities.Payment(
        externalPaymentId = externalPaymentId,
        paymentDateTime = paymentDateTime,
        stoppedDateTime = stoppedDateTime,
        requestedBy = requestedBy,
        currency = currency,
        currencyAmount = currencyAmount,
        paymentTarget = paymentTarget,
        paymentMetadata = paymentMetadata
    ).apply {
        id = Random.nextLong()
        // allows for passing of null to this function so that paymentTransactions can be attached later
        // if paymentTransactions is not explicitly set to null a random transaction will be generated for the test
        if (paymentTransactions != null) this.paymentTransactions.add(getPaymentTransaction(this))
    }

internal fun getPaymentTarget(
    targetKey: String = "targetKey",
    targetType: String = STRIPE_PAYMENT_TARGET_TYPE,
    payments: MutableSet<dev.odmd.platform.springcdk.domain.entities.Payment>? = mutableSetOf()
) = dev.odmd.platform.springcdk.domain.entities.PaymentTarget(
    targetType = targetType,
    targetKey = targetKey,
    payments = payments ?: mutableSetOf()
).apply {
    id = Random.nextLong()
    // allows for passing of null to this function so that payments can be attached later
    // if payments is not explicitly set to null a random payment will be generated for the test
    if (payments != null) this.payments.add(getPayment(this))
}

internal fun getPaymentTransaction(
    payment: dev.odmd.platform.springcdk.domain.entities.Payment,
    externalPaymentTransactionId: String = "external-payment-transaction-auth",
    paymentTransactionType: TransactionType = TransactionType.AUTH,
    source: String = "source",
    currencyAmount: BigDecimal = "150".toBigDecimal(),
    transactionStatus: TransactionStatus = TransactionStatus.SUCCESS,
    paymentProfile: dev.odmd.platform.springcdk.domain.entities.PaymentProfile = getPaymentProfile(),
    reason: String = "reason",
) =
    PaymentTransaction(
        externalPaymentTransactionId = externalPaymentTransactionId,
        paymentTransactionType = paymentTransactionType,
        source = source,
        currencyAmount = currencyAmount,
        transactionStatus = transactionStatus,
        payment = payment,
        paymentProfile = paymentProfile,
        reason = reason,
        paymentTransactionMetadata = null
    ).apply {
        id = Random.nextLong()
        createdDateTime = Instant.EPOCH
    }
