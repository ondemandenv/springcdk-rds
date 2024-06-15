package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.PaymentMetadata
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PaymentMetadataRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.PaymentMetadata, Long> {
}
