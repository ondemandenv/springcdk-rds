package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.PaymentTransactionMetadata
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentTransactionMetadataRepository : JpaRepository<PaymentTransactionMetadata, Long> {
}

