package dev.odmd.platform.springcdk.model.v1

data class InstallmentPaymentMethod(
    val type: dev.odmd.platform.springcdk.model.v1.InstallmentPaymentMethod.InstallmentPaymentMethodTypes,
    val creditCard: dev.odmd.platform.springcdk.model.v1.CreditCardPaymentMethod?
) {

    enum class InstallmentPaymentMethodTypes(val type: String) {
        CREDIT_CARD("CREDIT_CARD"),
        SAVED_CREDIT_CARD("SAVED_CREDIT_CARD")
    }
}

