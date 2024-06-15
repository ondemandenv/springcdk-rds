package dev.odmd.platform.springcdk.eventspublisher.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import dev.odmd.platform.springcdk.common.RequestHeaders
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.common.getLogger
import dev.odmd.platform.springcdk.eventspublisher.config.BillingConfig
import dev.odmd.platform.springcdk.eventspublisher.model.DomainEventRequestDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.*
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class BillingService(
    private val config: BillingConfig,
    private val restTemplate: RestTemplate,
    private val retryTemplate: RetryTemplate
) {
    @Autowired(required = true)
    constructor(
        config: BillingConfig,
        retryTemplate: RetryTemplate,
        restTemplateBuilder: RestTemplateBuilder
    ) : this(
        config = config,

        restTemplate = restTemplateBuilder.withBillingObjectMapper().build(),
        retryTemplate = retryTemplate
    )

    companion object {
        val logger = getLogger<BillingService>()

        fun objectMapper() = ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setTimeZone(TimeZone.getTimeZone("UTC"))
    }

    fun publishDomainEvent(httpMethod: HttpMethod, customerId: String, request: DomainEventRequestDto): ResponseEntity<Void> {
        logger.event(
            "PublishEventToBillingAPI.requestStarted",
            mapOf(
                "eventId" to request.eventId,
                "eventName" to request.eventName,
                "aggregateId" to request.aggregateId,
                "customerId" to customerId
            )
        )

        val headers = HttpHeaders().apply {
            set(RequestHeaders.ODMD_API_KEY, config.apiKey)
            set(RequestHeaders.ODMD_CUSTOMER_ID, customerId)
            contentType = MediaType.APPLICATION_JSON
        }

        val entity = HttpEntity(request, headers)

        val response = retryTemplate.execute<ResponseEntity<Void>, Exception> {
            process(httpMethod, entity)
        }

        logger.event(
            "PublishEventToBillingAPI.requestEnded",
            mapOf(
                "eventId" to request.eventId,
                "eventName" to request.eventName,
                "aggregateId" to request.aggregateId,
                "customerId" to customerId
            )
        )

        return response
    }


    fun process(httpMethod: HttpMethod, entity: HttpEntity<DomainEventRequestDto>): ResponseEntity<Void> {
        val customerId = entity.headers[RequestHeaders.ODMD_CUSTOMER_ID]
        try {
            val response = restTemplate.postForEntity(
                config.url,
                entity,
                Void::class.java
            )

            if (!response.statusCode.is2xxSuccessful) {
                logger.error("Cannot publish event to billingService statusCode: ${response.statusCode} for eventId: ${entity.body?.eventId}, eventName: ${entity.body?.eventName}, aggregateId: ${entity.body?.aggregateId}, customerId: $customerId")
                throw Exception("Cannot publish event to billingService statusCode: ${response.statusCode} for eventId: ${entity.body?.eventId}, eventName: ${entity.body?.eventName}, aggregateId: ${entity.body?.aggregateId}, customerId: $customerId")
            }

            return response
        } catch (ex: Exception) {
            logger.error(
                "Cannot publish event to billingService. eventId: ${entity.body?.eventId}, eventName: ${entity.body?.eventName}, aggregateId: ${entity.body?.aggregateId}, customerId: $customerId. Error: $ex")
            throw Exception(
                "Cannot publish event to billingService. eventId: ${entity.body?.eventId}, eventName: ${entity.body?.eventName}, aggregateId: ${entity.body?.aggregateId}, customerId: $customerId",
                ex
            )
        }
    }
}

fun RestTemplateBuilder.withBillingObjectMapper(): RestTemplateBuilder =
    messageConverters(
        MappingJackson2HttpMessageConverter(BillingService.objectMapper())
    )
