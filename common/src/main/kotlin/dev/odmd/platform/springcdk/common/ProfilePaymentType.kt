package dev.odmd.platform.springcdk.common


typealias ProfilePaymentTypeIdentifier = String

enum class ProfilePaymentType(val type: ProfilePaymentTypeIdentifier) {
    CREDIT_CARD("CREDIT_CARD")
}
