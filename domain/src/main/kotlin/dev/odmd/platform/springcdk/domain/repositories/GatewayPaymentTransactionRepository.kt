package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.GatewayPaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.ResponseStatus
import javax.persistence.EntityNotFoundException

@Repository
interface GatewayPaymentTransactionRepository : JpaRepository<GatewayPaymentTransaction, Long> {

    fun findByGatewayIdentifier(gatewayIdentifier: String): GatewayPaymentTransaction?

}

@ResponseStatus(HttpStatus.NOT_FOUND)
class PaymentGatewayTransactionNotFoundException(val gatewayIdentifier: String)
    : EntityNotFoundException("Couldn't find gatewayIdentifier [$gatewayIdentifier]")
