package dev.odmd.platform.springcdk.authorization

import dev.odmd.platform.springcdk.domain.repositories.GatewayPaymentTransactionRepository
import org.springframework.stereotype.Service


@Service
class PaymentGatewayTransactionAuthorization(private val gatewayPaymentTransactionRepository: GatewayPaymentTransactionRepository) :
    ResourceOwnerAuthorization() {
    override fun getCustomerIdByResourceId(resourceId: String): String? =
        gatewayPaymentTransactionRepository.findByGatewayIdentifier(resourceId)?.paymentTransaction?.paymentProfile?.customerId
}