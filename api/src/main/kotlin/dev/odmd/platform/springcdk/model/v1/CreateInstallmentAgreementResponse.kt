package dev.odmd.platform.springcdk.model.v1

data class CreateInstallmentAgreementResponse(

    val status: InstallmentAgreement.InstallmentAgreementStatus,

    val requestId: RequestId,
    val currencyCode: String,
    val targetType: String,
    val targetKey: String,

    val lineItemIds: List<String>
)
