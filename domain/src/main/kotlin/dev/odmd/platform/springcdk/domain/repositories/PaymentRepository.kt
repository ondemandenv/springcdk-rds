package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.Payment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PaymentRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.Payment, Long> {

    @EntityGraph(value = "payments.transactions")
    fun findByExternalPaymentId(externalId: String): dev.odmd.platform.springcdk.domain.entities.Payment?
    fun findByExternalPaymentIdIn(externalIds: Collection<String>): Collection<dev.odmd.platform.springcdk.domain.entities.Payment>

    @Query("select p from PaymentProfile pp " +
            "inner join PaymentTransaction pt on pp.id = pt.paymentProfile.id " +
            "inner join Payment p on p.id = pt.payment.id " +
            "where pp.customerId = ?1")
    fun findPaymentsByCustomerId(
        customerId: String,
        pageable: Pageable
    ): Page<dev.odmd.platform.springcdk.domain.entities.Payment>

}
