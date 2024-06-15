package dev.odmd.platform.springcdk.authorization

import dev.odmd.platform.springcdk.domain.repositories.PaymentProfileRepository
import org.springframework.stereotype.Service

@Service
class PaymentProfileAuthorization(private val paymentProfileRepository: PaymentProfileRepository) :
    ResourceOwnerAuthorization() {
    override fun getCustomerIdByResourceId(resourceId: String): String? =
        paymentProfileRepository.findByExternalIdAndIsActiveTrue(resourceId)?.customerId
}
