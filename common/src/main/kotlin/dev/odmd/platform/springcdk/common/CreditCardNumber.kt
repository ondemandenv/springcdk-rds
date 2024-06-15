package dev.odmd.platform.springcdk.common

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.swagger.v3.oas.annotations.media.Schema

@JsonDeserialize(using = CreditCardNumberJsonDeserializer::class)
@Schema(type = "string", example = "4111111111111111")
/**
 * Container class for credit card numbers. Allows us to easily audit all places where a credit card number is accessed
 *
 * We use a [CharArray] for credit card number storage to allow us to overwrite the memory containing the ccn after
 * we're done with it. Using a [String] would not allow us to do that.
 *
 * JSON serializer intentionally omitted - we want all access to the underlying number to be explicit
 */
data class CreditCardNumber (val accountNumber: CharArray) : AutoCloseable, FirstCreditCardDigitProvider {

    val lastFour: String
        get() {
            return String(accountNumber).mask(MaskingMode.LAST_FOUR)
        }

    val firstDigitAsString: String
        get() = accountNumber.first().toString()

    override val firstDigit: Int
        get() = accountNumber.first().digitToInt()

    override fun toString(): String {
        return "CreditCardNumber(lastFour='$lastFour', firstDigit='$firstDigitAsString')"
    }

    /**
     * Zero out the [accountNumber] field.
     *
     * The JVM does not guarantee that [finalize] will be called on any particular object,
     * so [close] should be used whenever possible.
     */
    override fun close() {
        for (i in accountNumber.indices) {
            accountNumber[i] = '\u0000'
        }
    }

    /**
     * Reduces risk of information exposure if the caller does not or cannot call [close].
     */
    protected fun finalize() {
        close()
    }

    // Need to override equals/hashcode for data classes that contain raw arrays
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CreditCardNumber

        if (!accountNumber.contentEquals(other.accountNumber)) return false

        return true
    }

    override fun hashCode(): Int {
        return accountNumber.contentHashCode()
    }
}

class CreditCardNumberJsonDeserializer : StdDeserializer<CreditCardNumber>(CreditCardNumber::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): CreditCardNumber {
        return CreditCardNumber(p.readValueAs(CharArray::class.java))
    }
}

/**
 * This serializer is *intentionally omitted* to prevent leaking credit card numbers.
 *
 * If you want to use this serializer, for example as part of sending an API request,
 * register [CreditCardNumberJsonSerializer.module] with your [ObjectMapper].
 */
class CreditCardNumberJsonSerializer :
    StdSerializer<CreditCardNumber>(CreditCardNumber::class.java) {
    override fun serialize(
        value: CreditCardNumber,
        gen: JsonGenerator,
        provider: SerializerProvider
    ) {
        gen.writeString(value.accountNumber, 0, value.accountNumber.size)
    }

    companion object {
        val module
            get() = SimpleModule().apply {
                addSerializer(CreditCardNumber::class.java, CreditCardNumberJsonSerializer())
            }
    }
}
