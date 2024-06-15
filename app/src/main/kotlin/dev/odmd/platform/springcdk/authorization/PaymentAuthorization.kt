package dev.odmd.platform.springcdk.authorization

import dev.odmd.platform.springcdk.domain.repositories.PaymentRepository
import org.springframework.stereotype.Service

@Service
class PaymentAuthorization (
    val paymentRepository: dev.odmd.platform.springcdk.domain.repositories.PaymentRepository
) : dev.odmd.platform.springcdk.authorization.ResourceOwnerAuthorization() {

    override fun getCustomerIdByResourceId(resourceId: String): String?
        = paymentRepository.findByExternalPaymentId(resourceId)?.paymentTransactions?.firstOrNull()?.paymentProfile?.customerId
}