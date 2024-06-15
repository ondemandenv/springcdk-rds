package dev.odmd.platform.springcdk.model.v1

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import javax.validation.*
import kotlin.reflect.KClass

sealed interface PaymentMethodData

/**
 * Data class that allows API clients to specify one of the available [PaymentMethodTypes].
 *
 * When interacting with this class in application code, don't call this constructor or access
 * the declared fields directly. Use the constructors & getter functions in the class body instead.
 *
 * Ideally the constructor & fields would be declared `internal`
 */
data class PaymentMethod(
    val type: dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes,

    @field:NotExpired
    @field:Valid
    val creditCard: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod?,

    val savedCreditCard: dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod?
) {
    constructor(creditCard: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod) : this(
        type = dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.CREDIT_CARD,
        creditCard = creditCard,
        savedCreditCard = null
    )

    constructor(savedCreditCard: dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod) : this(
        type = dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.SAVED_CREDIT_CARD,
        creditCard = null,
        savedCreditCard = savedCreditCard
    )

    /**
     * @return [PaymentMethodData] based on the receiver's [type] field.
     * @throws [PaymentMethodException] if the expected payment method data is null.
     */
    fun unwrap(): PaymentMethodData =
        when(type) {
            dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.CREDIT_CARD -> creditCardOrThrow()
            dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.SAVED_CREDIT_CARD -> savedCreditCardOrThrow()
        }

    fun creditCardOrThrow(): dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod = getMethodIfValid(creditCard, dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.CREDIT_CARD)

    fun savedCreditCardOrThrow(): dev.odmd.platform.springcdk.model.v1.SavedCreditCardPaymentMethod = getMethodIfValid(savedCreditCard, dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes.SAVED_CREDIT_CARD)

    private fun <T: PaymentMethodData> getMethodIfValid(
        paymentMethod: T?,
        expectedType: dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes
    ): T =
        if (type == expectedType) {
            paymentMethod ?: throw InvalidPaymentMethodException(type)
        } else {
            throw UnexpectedPaymentMethodException(
                expectedType = expectedType,
                actualType = type
            )
        }
}

abstract class PaymentMethodException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidPaymentMethodException(val type: dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes) :
    PaymentMethodException("Specified payment method type $type, but the $type field was null.")


class UnexpectedPaymentMethodException(
    val expectedType: dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes,
    val actualType: dev.odmd.platform.springcdk.model.v1.PaymentMethodTypes
) : PaymentMethodException("Expected payment method '$expectedType', got $actualType")


@MustBeDocumented
@Constraint(validatedBy = [PaymentMethodValidator::class])
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.RUNTIME)
@ReportAsSingleViolation
annotation class ValidPaymentMethod(
    val message: String = "Must specify only the payment method information matching the type.",
    val groups: Array<KClass<out Any>> = [],
    val payload: Array<KClass<out Any>> = []
)
class PaymentMethodValidator : ConstraintValidator<ValidPaymentMethod, PaymentMethod> {
    override fun isValid(value: PaymentMethod?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return false
        }
        return try {
            value.unwrap()
            true
        } catch (e: PaymentMethodException) {
            context.disableDefaultConstraintViolation()
            context
                .buildConstraintViolationWithTemplate(e.message)
                .addConstraintViolation()
            false
        }
    }
}
