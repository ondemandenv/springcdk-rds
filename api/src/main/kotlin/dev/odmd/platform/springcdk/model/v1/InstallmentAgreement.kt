package dev.odmd.platform.springcdk.model.v1

data class InstallmentAgreement(
    val id: String,
    val status: InstallmentAgreementStatus,
    val targetType: String,
    val targetKey: String,
    val currency: String,
    val installments: List<Installment>,
    val lineItemDtos: List<LineItemDto>
) {

    enum class InstallmentAgreementStatus(val type: String) {
        ACTIVE("ACTIVE"),
        COMPLETE("COMPLETE"),
        DECLINED("DECLINED"),
        SUSPENDED("SUSPENDED"),
    }

    data class Installment(val date: String, val amount: String, val status: String, val paymentIds: List<String>) {
        enum class InstallmentStatus(val type: String) {
            SCHEDULED("SCHEDULED"),
            DUE("DUE")
        }
    }
}
