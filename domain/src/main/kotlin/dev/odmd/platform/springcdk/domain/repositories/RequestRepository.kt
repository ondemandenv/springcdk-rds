package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.PaymentRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface RequestRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.PaymentRequest, String> {
}
