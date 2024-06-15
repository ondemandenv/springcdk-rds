package dev.odmd.platform.springcdk.model.v1.validation

import org.springframework.stereotype.Component
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.ReportAsSingleViolation
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [BillingDescriptorValidator::class])
@Target(
    AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY_GETTER
)
@Retention(AnnotationRetention.RUNTIME)
@ReportAsSingleViolation
annotation class ValidBillingDescriptor(
    val message: String = "Invalid billing descriptor. Must be between 5-17 characters, only latin alphabet, at least one letter, cannot contain any of <>\\'\"*",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)

/**
 * Requirements come from this page: https://stripe.com/docs/account/statement-descriptors
 *
 * That page specifies a max length of 22, but we use 5 for the prefix "LZC* " which we cannot change and is automatically added (by Stripe)
 */
@Component
class BillingDescriptorValidator : ConstraintValidator<ValidBillingDescriptor, String> {
    override fun isValid(value: String?, context: ConstraintValidatorContext?): Boolean {
        if (value == null) {
            return true
        }

        if (value.length < 5 || value.length > 17) {
            return false
        }

        if (value.contains(Regex("[<>\\\\'\"*]"))) {
            return false
        }

        if (value.contains(Regex("[^\\p{ASCII}]"))) {
            return false
        }

        if (!value.contains(Regex("[A-Za-z]"))) {
            return false
        }

        return true
    }
}
