package dev.odmd.platform.springcdk.common

import java.math.BigDecimal
import java.util.*
import javax.money.Monetary
import javax.money.MonetaryAmount

fun MonetaryAmount.toScaledLong(): Long =
    scaleByPowerOfTen(currency.defaultFractionDigits)
        .number
        .numberValueExact(BigDecimal::class.java)
        .toLong()

fun scaledLongToMonetaryAmount(currencyString: String, amount: Long): MonetaryAmount {
    val currency = Currency.getInstance(currencyString)
    if (currency.defaultFractionDigits == 0) {
        //special case as when defaultFractionDigits is zero we want to avoid converting into a Double due the way the Math.pow function works
        return Monetary.getDefaultAmountFactory().setCurrency(currencyString).setNumber(amount).create()
    }
    return Monetary.getDefaultAmountFactory().setCurrency(currencyString)
        .setNumber(amount)
        .create()
        .scaleByPowerOfTen(-currency.defaultFractionDigits)
}
