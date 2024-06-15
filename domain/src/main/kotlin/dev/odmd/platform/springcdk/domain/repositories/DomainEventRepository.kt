package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.DomainEvent
import dev.odmd.platform.springcdk.domain.entities.DomainEventMetadata
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DomainEventRepository: JpaRepository<DomainEvent, String> {
    fun findByProcessedAtIsNull(
        sort: Sort
    ): List<DomainEventMetadata>

    fun findByUuid(uuid: String): DomainEvent?

    fun findByCustomerId(customerId: String): List<DomainEvent>
}