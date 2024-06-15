package dev.odmd.platform.springcdk.model.v1

data class CreateInstallmentAgreementRequest(
    val requestId: dev.odmd.platform.springcdk.model.v1.RequestId,
    val currencyCode: String,
    val targetType: String,
    val targetKey: String,
    val reason: String,
    val source: String,
    val lineItemDtos: List<dev.odmd.platform.springcdk.model.v1.LineItemDto>,

    val totalAmount: String,
    val paymentMethod: dev.odmd.platform.springcdk.model.v1.InstallmentPaymentMethod,

    val installments: List<dev.odmd.platform.springcdk.model.v1.CreateInstallmentAgreementRequest.InstallmentListItem>

) {
    data class InstallmentListItem(val dueDate: String, val amount: String)
}
