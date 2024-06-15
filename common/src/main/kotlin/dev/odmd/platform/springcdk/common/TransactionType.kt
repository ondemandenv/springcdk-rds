package dev.odmd.platform.springcdk.common

enum class TransactionType(val type: String) {
    AUTH_CAPTURE("AUTH_CAPTURE"),
    CAPTURE("CAPTURE"),
    REFUND("REFUND"),
    AUTH("AUTH");

    companion object {
        val ALL_AUTH_TYPES = setOf(AUTH_CAPTURE, AUTH)
    }
}
