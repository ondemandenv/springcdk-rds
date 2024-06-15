package dev.odmd.platform.springcdk.eventspublisher.publisher

import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.domain.repositories.DomainEventRepository
import dev.odmd.platform.springcdk.eventspublisher.model.DomainEventRequestDto
import dev.odmd.platform.springcdk.eventspublisher.service.BillingService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PaymentEventPublisher(
    val domainEventRepository: DomainEventRepository,
    val billingService: BillingService,
    val constantTransitContextService: ConstantTransitContextService
) {

    companion object {
        val logger = LoggerFactory.getLogger(PaymentEventPublisher::class.java)
    }

    @Scheduled(fixedDelayString = "\${app.publish-events-scheduled}")
    fun publishEvent() {
        try {
            logger.info("PublishedEvent.Started")

            val domainEventMetadata =
                domainEventRepository
                    .findByProcessedAtIsNull(Sort.by("sequenceNumber").ascending())
                    .firstOrNull()
                    ?: return
            constantTransitContextService.setContext(domainEventMetadata.customerId.encodeToByteArray())
            val domainEvent = domainEventRepository.findByUuid(domainEventMetadata.uuid) ?: return

            val domainEventRequestDto = DomainEventRequestDto(
                eventId = domainEvent.uuid,
                eventName = domainEvent.eventName,
                aggregateId = domainEvent.aggregateId,
                eventAt = domainEvent.createdAt,
                payload = domainEvent.payload
            )
            val response = billingService.publishDomainEvent(HttpMethod.POST, domainEvent.customerId, domainEventRequestDto)

            if (response.statusCode == HttpStatus.OK) {
                domainEvent.processedAt = Instant.now()
                domainEventRepository.saveAndFlush(domainEvent)

                logger.event(
                    "PublishedEvent.Successfully",
                    mapOf(
                        "eventId" to domainEvent.uuid,
                        "eventName" to domainEvent.eventName,
                        "aggregateId" to domainEvent.aggregateId
                    )
                )
            }
        } catch (ex: Exception) {
            logger.error("Error occur while publishing a event. Error: $ex")
        }
    }
}
