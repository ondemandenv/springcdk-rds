package dev.odmd.platform.springcdk.security

class SpelExpressions {
    companion object {
        const val PAYMENT_PROFILE_IDS =
            "#authorizePaymentRequest.paymentDtos.![paymentMethod?.savedCreditCard?.paymentProfileId]"
        const val FIRST_PAYMENT_PROFILE_ID = "returnObject.^[true]?.paymentProfileId"
    }
}