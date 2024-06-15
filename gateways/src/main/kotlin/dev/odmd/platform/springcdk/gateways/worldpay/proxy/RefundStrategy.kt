package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ValueNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import dev.odmd.platform.springcdk.common.RefundType
import dev.odmd.platform.springcdk.common.scaledLongToMonetaryAmount
import dev.odmd.platform.springcdk.gateways.GatewayRefundRequest
import dev.odmd.platform.springcdk.gateways.Gateways
import dev.odmd.platform.springcdk.gateways.PaymentGatewaySupplier
import com.litle.sdk.generate.Credit
import com.litle.sdk.generate.CreditResponse
import com.litle.sdk.generate.ObjectFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory

@Service
internal class RefundStrategy(private val paymentGatewaySupplier: PaymentGatewaySupplier) : ProxyStrategy {
    override val operationName = "credit"
    private val xmlMapper = XmlMapper().findAndRegisterModules()
    private val jaxbFactory = ObjectFactory()

    override fun execute(value: JsonNode, entityId: String, defaultGateway: Gateways): JAXBElement<CreditResponse> {
        val gatewayTransactionId =
            value["litleTxnId"]?.asText(null) ?: throw ProxiedRequestMissingParametersException("litleTxnId")
        (value as ObjectNode).set<ValueNode>("litleTxnId", JsonNodeFactory.instance.numberNode(0L))
        val refund = xmlMapper.convertValue<Credit>(value)
        val gateway = decodeTransactionIdGateway(gatewayTransactionId)
        val gatewayService = paymentGatewaySupplier.get(gateway)
        val refundResponse = gatewayService.refund(
            dev.odmd.platform.springcdk.gateways.GatewayRefundRequest(
                entityId,
                refund.enhancedData?.customerReference
                    ?: throw ProxiedRequestMissingParametersException("enhancedData.customerReference"),
                UUID.randomUUID().toString(),
                RefundType.REVERSE,
                scaledLongToMonetaryAmount(
                    "USD",
                    refund.amount ?: throw ProxiedRequestMissingParametersException("amount")
                ),
                gatewayTransactionId,
                null
            )
        )

        return serialize(CreditResponse().apply {
            litleTxnId = refundResponse.gatewayIdentifier.toLongOrNull() ?: 0L
            orderId = refund.orderId
            response = mapGatewayResponseToWorldPayResponseCode(refundResponse)
            responseTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
            postDate =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(refundResponse.transactionDatetime.toString())
                    .toGregorianCalendar()
            message = mapGatewayResponseToWorldPayMessage(refundResponse)
            reportGroup = refund.reportGroup
        })
    }

    fun serialize(obj: CreditResponse): JAXBElement<CreditResponse> {
        return jaxbFactory.createCreditResponse(obj)
    }
}
