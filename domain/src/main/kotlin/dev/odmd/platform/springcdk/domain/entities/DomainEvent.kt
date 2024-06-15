package dev.odmd.platform.springcdk.domain.entities

import org.hibernate.annotations.Type
import java.time.Instant
import java.util.*
import javax.persistence.*

interface DomainEventMetadata {
    var uuid: String
    var customerId: String
}

@Table(name = "domain_events")
@Entity
class DomainEvent(

    var eventName: String,

    var aggregateId: String,

    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var payload: DomainEventPayload,

    var eventAt: Instant = Instant.now(),

    var customerId: String,
) {

    @Id
    var uuid: String = UUID.randomUUID().toString();

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = true)
    var processedAt: Instant? = null

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(insertable = false, updatable = false)
    var sequenceNumber: Long = 0

}