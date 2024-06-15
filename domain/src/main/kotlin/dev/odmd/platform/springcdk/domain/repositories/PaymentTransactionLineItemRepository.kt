package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentTransactionLineItemRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.PaymentTransactionLineItem, Long> {

}
