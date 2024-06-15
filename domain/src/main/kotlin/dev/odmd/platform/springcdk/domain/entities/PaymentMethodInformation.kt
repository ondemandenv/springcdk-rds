package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * [JsonSubTypes][com.fasterxml.jackson.annotation.JsonSubTypes] information is provided by `jackson-module-kotlin`,
 * which can generate the type information because [PaymentMethodInformation] is sealed.
 *
 * As configured, a field called `"@type"` is added to the serialized object with the simple (unqualified) name of the
 * class as its value. For example, for [CreditCardPaymentMethodInformation] all serialized objects will have a field
 * that looks like this:
 *
 * `"@type": "CreditCardPaymentMethodInformation"`
 *
 * That means that it is **not safe to rename** any subclasses of [PaymentMethodInformation].
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME
)
sealed interface PaymentMethodInformation
