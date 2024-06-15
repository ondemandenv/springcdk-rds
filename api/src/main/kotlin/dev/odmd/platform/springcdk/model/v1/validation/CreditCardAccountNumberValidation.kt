package dev.odmd.platform.springcdk.model.v1.validation

import dev.odmd.platform.springcdk.common.CreditCardNumber
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [dev.odmd.platform.springcdk.model.v1.validation.CreditCardAccountNumberValidator::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@ReportAsSingleViolation
annotation class ValidCreditCardNumber(
    val message: String = "Invalid credit card account number",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

@Component
class CreditCardAccountNumberValidator : ConstraintValidator<dev.odmd.platform.springcdk.model.v1.validation.ValidCreditCardNumber, CreditCardNumber> {
    override fun isValid(creditCardNumber: CreditCardNumber?, cxt: ConstraintValidatorContext): Boolean {
        if (creditCardNumber == null) {
            return true
        }
        if (dev.odmd.platform.springcdk.model.v1.validation.CreditCardAccountNumberValidator.Companion.isTestCardNumber(
                creditCardNumber.accountNumber
            )
        ) {
            return true
        }

        val nDigits: Int = creditCardNumber.accountNumber.size
        var nSum = 0
        var isSecond = false
        for (i in nDigits - 1 downTo 0) {
            var nextDigit: Int = creditCardNumber.accountNumber.elementAt(i) - '0'
            if (isSecond) {
                nextDigit *= 2
            }
            // We add two digits to handle
            // cases that make two digits
            // after doubling
            nSum += nextDigit / 10
            nSum += nextDigit % 10
            isSecond = !isSecond
        }
        return nSum % 10 == 0
    }

    private companion object {
        lateinit var testCardNumbers: List<String>

        fun isTestCardNumber(cardNumber: CharArray) =
            dev.odmd.platform.springcdk.model.v1.validation.CreditCardAccountNumberValidator.Companion.testCardNumbers.any { testCardNumber ->
                cardNumber.contentEquals(testCardNumber.toCharArray())
            }
    }

    @Autowired
    fun setTestCardNumbers(config: dev.odmd.platform.springcdk.model.v1.validation.CreditCardValidationConfiguration) {
        dev.odmd.platform.springcdk.model.v1.validation.CreditCardAccountNumberValidator.Companion.testCardNumbers = config.testCardNumbers
    }

}

@ConfigurationProperties(prefix="app.credit-card-validation")
@Component
class CreditCardValidationConfiguration {
    var testCardNumbers: List<String> = emptyList()
}
