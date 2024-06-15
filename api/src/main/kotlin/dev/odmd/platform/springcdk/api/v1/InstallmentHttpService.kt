package dev.odmd.platform.springcdk.api.v1

import dev.odmd.platform.springcdk.model.v1.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@Tag(name = "Installments", description = "Installment Agreement Management")
@RequestMapping("installment-agreement")
interface InstallmentHttpService {
    @PostMapping
    @Operation(
        summary = "Create installment agreement",
        description = "\${installment-service.create-installment-agreement}"
    )
    fun createInstallmentAgreement(@RequestBody request: CreateInstallmentAgreementRequest): CreateInstallmentAgreementResponse

    @GetMapping("{installmentAgreementId}")
    @Operation(summary = "get installment agreement", description = "\${installment-service.get-installment-agreement}")
    fun getInstallmentAgreement(@PathVariable installmentAgreementId: String): InstallmentAgreement


    @PutMapping("{installmentAgreementId}/suspend")
    @Operation(
        summary = "suspend installment agreement",
        description = "\${installment-service.suspend-installment-agreement}"
    )
    fun suspendInstallmentAgreement(@PathVariable installmentAgreementId: String): InstallmentAgreement

    @PutMapping("{installmentAgreementId}/resume")
    @Operation(
        summary = "resume installment agreement",
        description = "\${installment-service.resume-installment-agreement}"
    )
    fun resumeInstallmentAgreement(@PathVariable installmentAgreementId: String): InstallmentAgreement


    @PutMapping("{installmentAgreementId}/paymentMethod")
    @Operation(
        summary = "update installment payment method",
        description = "\${installment-service.update-installment-payment-method}"
    )
    fun updateInstallmentPaymentMethod(
        @PathVariable installmentAgreementId: String,
        @RequestBody paymentMethod: InstallmentPaymentMethod
    ): InstallmentAgreement

    @PutMapping("{installmentAgreementId}/applyPayment")
    @Operation(
        summary = "Apply a payment method to any of an agreement’s pending installments.",
        description = "\${installment-service.apply-installment-payment-method}"
    )
    fun applyInstallmentPaymentMethod(
        @PathVariable installmentAgreementId: String,
        @RequestBody payment: InstallmentAppyPaymentMethod
    ): InstallmentAgreement

}
