package dev.odmd.platform.springcdk.model.v1

import java.time.DateTimeException
import java.time.YearMonth
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

//This custom validation is written for validating Credit Card expiry month and expiry year validation.
//We make sure Credit card expiry month always future month from current month and expiry year should be current or future year.
@MustBeDocumented
@Constraint(validatedBy = [CreditCardExpiryValidator::class])
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.RUNTIME)
@ReportAsSingleViolation
annotation class NotExpired(
    val message: String = "Specified month and year are not in the future.",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

class CreditCardExpiryValidator : ConstraintValidator<NotExpired, dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod> {
    override fun isValid(
        creditCardPaymentMethod: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod?,
        cxt: ConstraintValidatorContext
    ): Boolean {
        if (creditCardPaymentMethod == null) {
            return true
        }
        val currentYearMonth = YearMonth.now()
        val year = if (creditCardPaymentMethod.expYear < 100) {
            creditCardPaymentMethod.expYear + 2000 // this will break in 78 years. That's probably fine
        } else {
            creditCardPaymentMethod.expYear
        }
        val inputDate = try {
            YearMonth.of(
                year,
                creditCardPaymentMethod.expMonth
            )
        } catch (e: DateTimeException) {
            return false
        }
        return inputDate >= currentYearMonth
    }
}
