package dev.odmd.platform.springcdk.common

enum class TransactionStatus {
    PENDING,//before calling gateway

    //gateway result
    SUCCESS,
    DECLINED,
    PENDING_GATEWAY,
    STOPPED,
    AUTH_CANCELLED
}
