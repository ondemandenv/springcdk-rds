package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.odmd.platform.springcdk.gateways.Gateways
import dev.odmd.platform.springcdk.gateways.PaymentGatewayConfiguration
import dev.odmd.platform.springcdk.gateways.worldpay.WorldPayConfiguration
import com.litle.sdk.generate.LitleOnlineResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.ByteArrayOutputStream
import java.io.Reader
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

@Service
class WorldPayProxyService internal constructor(
    private val worldPayConfiguration: WorldPayConfiguration,
    private val restTemplate: RestTemplate = RestTemplate(),
    tokenizationStrategy: TokenizationStrategy,
    authorizationStrategy: AuthorizationStrategy,
    saleStrategy: SaleStrategy,
    refundStrategy: RefundStrategy,
    gatewayConfiguration: PaymentGatewayConfiguration,
) {
    private final val defaultOutputBufferSizeBytes = 512
    private val logger = LoggerFactory.getLogger(WorldPayProxyService::class.java)
    private val xmlMapper = XmlMapper().findAndRegisterModules()
    private val jaxbXmlMarshaller = JAXBContext.newInstance(LitleOnlineResponse::class.java).createMarshaller().apply {
        this.setProperty(Marshaller.JAXB_FRAGMENT, true)
    }
    private val tokenizationDefaultGateway = gatewayConfiguration.proxyDefault
    private val proxyStrategies =
        listOf(
            tokenizationStrategy,
            authorizationStrategy,
            saleStrategy,
            refundStrategy
        ).associateBy { it.operationName }

    fun proxyRequest(httpMethod: HttpMethod, reader: Reader): ResponseEntity<ByteArray> {
        val request = deserializeRequest(reader)

        val entityId = request.findValuesAsText("customerId").firstOrNull { !it.isNullOrBlank() } ?: ""

        val tokens = request.findValuesAsText("litleToken")
        if (tokens.isNotEmpty()) {
            if (tokens.size > 1) {
                throw ProxiedRequestUnsupportedOperationException("multiple litleToken fields found")
            }

            val tokenGateway = decodeTokenGateway(tokens.first())
            if (tokenGateway == Gateways.WORLDPAY) {
                logger.info("Proxy pass-through for entity {$entityId}")
                return restTemplate.exchange(
                    worldPayConfiguration.url,
                    httpMethod,
                    HttpEntity(request),
                    ByteArray::class.java
                )
            }
        }

        request.fields().forEach {
            val matchingStrategy = proxyStrategies[it.key]
            if (matchingStrategy != null) {
                logger.info("Executing proxied ${matchingStrategy.operationName} request for entity {$entityId}")
                val strategyResponse = matchingStrategy.execute(it.value, entityId, tokenizationDefaultGateway)

                val litleResponse = LitleOnlineResponse().apply {
                    this.transactionResponse = strategyResponse
                    this.message = "Valid Format"
                    this.response = "0"
                    this.version = "8.3"
                }

                val byteWriter = ByteArrayOutputStream(defaultOutputBufferSizeBytes)
                jaxbXmlMarshaller.marshal(litleResponse, byteWriter)

                val outputString = byteWriter.toString(Charsets.UTF_8)
                val newOutString = outputString.replace("xmlns=\"http://www.litle.com/schema\"", "")

                return ResponseEntity.of(Optional.of(newOutString.toByteArray()))
            } else when (it.key) {
                // these are metadata no-ops
                "authentication", "version", "merchantId" -> {}
                else -> {
                    logger.error("Unsupported proxy operation ${it.key}")
                }
            }
        }

        throw ProxiedRequestUnsupportedOperationException(
            "Unable to find proxy operation to execute. Fields present include {${
                request.fields().iterator().asSequence().joinToString { it.key }
            }}"
        )
    }

    private fun deserializeRequest(reader: Reader): JsonNode {
        try {
            return xmlMapper.readTree(reader)
        } catch (e: JsonParseException) { // [JsonParseException] also covers XML parse exceptions
            logger.error("Error deserializing proxied WP request", e)
            throw ProxiedRequestDeserializationException(e)
        }
    }
}
