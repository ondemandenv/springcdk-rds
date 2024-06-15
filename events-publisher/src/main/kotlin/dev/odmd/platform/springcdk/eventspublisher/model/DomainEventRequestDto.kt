package dev.odmd.platform.springcdk.eventspublisher.model

import java.time.Instant

data class DomainEventRequestDto (
    val eventId: String,
    val eventName: String,
    val aggregateId: String,
    val eventAt: Instant,
    val payload: Any,
    val sequenceNumber: Long = 0L
)
