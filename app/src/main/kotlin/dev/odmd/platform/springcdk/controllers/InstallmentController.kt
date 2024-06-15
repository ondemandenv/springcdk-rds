package dev.odmd.platform.springcdk.controllers

import dev.odmd.platform.springcdk.api.v1.InstallmentHttpService
import dev.odmd.platform.springcdk.model.v1.*
import org.springframework.web.bind.annotation.RestController

@RestController
class InstallmentController : dev.odmd.platform.springcdk.api.v1.InstallmentHttpService {
    override fun createInstallmentAgreement(request: CreateInstallmentAgreementRequest): CreateInstallmentAgreementResponse {
        TODO("Not yet implemented")
    }

    override fun getInstallmentAgreement(installmentAgreementId: String): InstallmentAgreement {
        TODO("Not yet implemented")
    }

    override fun suspendInstallmentAgreement(installmentAgreementId: String): InstallmentAgreement {
        TODO("Not yet implemented")
    }

    override fun resumeInstallmentAgreement(installmentAgreementId: String): InstallmentAgreement {
        TODO("Not yet implemented")
    }

    override fun updateInstallmentPaymentMethod(
        installmentAgreementId: String,
        paymentMethod: dev.odmd.platform.springcdk.model.v1.InstallmentPaymentMethod
    ): InstallmentAgreement {
        TODO("Not yet implemented")
    }

    override fun applyInstallmentPaymentMethod(
        installmentAgreementId: String,
        payment: InstallmentAppyPaymentMethod
    ): InstallmentAgreement {
        TODO("Not yet implemented")
    }
}
