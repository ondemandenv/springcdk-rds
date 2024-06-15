package dev.odmd.platform.springcdk.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.AbstractDecimalMaxValidator
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.AbstractDecimalMinValidator
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.money.MonetaryAmount

/**
 * Represents a monetary amount of variable precision.
 *
 * The only arithmetic use-case we have for monetary amounts is summing various amounts to verify
 * sub-components against a total, so only the `plus` and `minus` operators are implemented.
 */
@JsonSerialize(using = CurrencyAmountJsonSerializer::class)
@JsonDeserialize(using = CurrencyAmountJsonDeserializer::class)
@Schema(type = "string", example = "0.00")
class CurrencyAmount {
    private val logger = LoggerFactory.getLogger(CurrencyAmount::class.java)
    val amount: BigDecimal

    constructor(stringAmount: String) : this(BigDecimal(stringAmount))

    constructor(amount: BigDecimal) {
        this.amount = scaleAmount(amount)
    }

    private fun scaleAmount(amt: BigDecimal): BigDecimal {
        return if (amt.scale() > 2) {
            logger.warn("Monetary amount with more than two decimal places passed. Rounding. ({})", amt)
            amt.setScale(2, RoundingMode.HALF_UP)
        } else {
            amt
        }
    }

    operator fun plus(otherAmount: CurrencyAmount): CurrencyAmount {
        return CurrencyAmount(amount.plus(otherAmount.amount))
    }

    operator fun minus(otherAmount: CurrencyAmount): CurrencyAmount {
        return CurrencyAmount(amount.minus(otherAmount.amount))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrencyAmount

        if (amount.compareTo(other.amount) != 0) return false

        return true
    }

    override fun hashCode(): Int {
        return amount.hashCode()
    }

    override fun toString(): String {
        return "MonetaryAmount(amount=$amount)"
    }
}

fun MonetaryAmount.toCurrencyAmount() = CurrencyAmount(number.numberValueExact(BigDecimal::class.java))

class CurrencyAmountJsonSerializer : StdSerializer<CurrencyAmount>(CurrencyAmount::class.java) {
    companion object {
        private val f = DecimalFormat("#0.00")
    }
    override fun serialize(value: CurrencyAmount, gen: JsonGenerator, provider: SerializerProvider?) {
        gen.writeString(f.format(value.amount))
    }
}

class CurrencyAmountJsonDeserializer : StdDeserializer<CurrencyAmount>(CurrencyAmount::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): CurrencyAmount {
        return CurrencyAmount(p.valueAsString)
    }
}

class CurrencyAmountDecimalMinValidator : AbstractDecimalMinValidator<CurrencyAmount>() {
    override fun compare(number: CurrencyAmount): Int {
        // null checking is handled in AbstractDecimalMinValidator#isValid
        return number.amount.compareTo(minValue)
    }
}

class CurrencyAmountDecimalMaxValidator : AbstractDecimalMaxValidator<CurrencyAmount>() {
    override fun compare(number: CurrencyAmount): Int {
        // null checking is handled in AbstractDecimalMaxValidator#isValid
        return number.amount.compareTo(maxValue)
    }
}
