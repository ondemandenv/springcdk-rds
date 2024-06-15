package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.PaymentProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentProfileRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.PaymentProfile, Long> {
    fun findByCustomerIdAndIsActiveTrue(customerId: String): List<dev.odmd.platform.springcdk.domain.entities.PaymentProfile>

    fun findByCustomerId(customerId: String): List<dev.odmd.platform.springcdk.domain.entities.PaymentProfile>

    fun findByExternalIdAndIsActiveTrue(externalId: String): dev.odmd.platform.springcdk.domain.entities.PaymentProfile?
    fun findByExternalIdInAndIsActiveTrue(externalIds: Set<String>): Set<dev.odmd.platform.springcdk.domain.entities.PaymentProfile>

    fun findByExternalId(externalId: String): dev.odmd.platform.springcdk.domain.entities.PaymentProfile?

    fun findByExternalIdInAndIsActive(externalIds: Set<String>, isActive: Boolean): Set<dev.odmd.platform.springcdk.domain.entities.PaymentProfile>
}
