package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.BillingInformation
import dev.odmd.platform.springcdk.common.CreditCardNumber
import dev.odmd.platform.springcdk.model.v1.validation.ValidCreditCardNumber
import io.swagger.v3.oas.annotations.media.Schema
import org.hibernate.validator.constraints.Length
import javax.validation.Valid

data class CreditCardPaymentMethod(
    @field:Valid
    @field:dev.odmd.platform.springcdk.model.v1.validation.ValidCreditCardNumber
    val creditCardNumber: CreditCardNumber,

    @field:Schema(type = "integer", example = "12")
    val expMonth: Int,
    @field:Schema(type = "integer", example = "34")
    val expYear: Int,

    @field:Length(min = 3, max = 4)
    @field:Schema(type = "string", example = "111")
    val cardVerificationValue: String?,

    val billingInformation: BillingInformation?
) : dev.odmd.platform.springcdk.model.v1.PaymentMethodData
