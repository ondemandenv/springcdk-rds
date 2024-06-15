package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.common.scaledLongToMonetaryAmount
import dev.odmd.platform.springcdk.common.toScaledLong
import dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest
import dev.odmd.platform.springcdk.gateways.Gateways
import dev.odmd.platform.springcdk.gateways.PaymentGatewaySupplier
import com.litle.sdk.generate.ObjectFactory
import com.litle.sdk.generate.Sale
import com.litle.sdk.generate.SaleResponse
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory

@Service
internal class SaleStrategy(private val paymentGatewaySupplier: PaymentGatewaySupplier) : ProxyStrategy {
    override val operationName: String = "sale"
    private val xmlMapper = XmlMapper().findAndRegisterModules()
    private val jaxbFactory = ObjectFactory()

    override fun execute(
        value: JsonNode,
        entityId: String,
        defaultGateway: Gateways
    ): JAXBElement<SaleResponse> {
        val sale = xmlMapper.convertValue<Sale>(value)

        val tokenNode = sale.token
            ?: throw ProxiedRequestMissingParametersException("token")

        val token = tokenNode.litleToken ?: throw ProxiedRequestMissingParametersException("token.litleToken")
        val gateway = decodeTokenGateway(token)
        val gatewayService = paymentGatewaySupplier.get(gateway)

        val billToNode = sale.billToAddress ?: throw ProxiedRequestMissingParametersException("billToAddress")
        val billingInformation = mapContactToBillingInformation(billToNode)

        val (expMonth, expYear) = decodeExpDate(tokenNode.expDate)

        val amount = sale.amount ?: throw ProxiedRequestMissingParametersException("amount")
        val monetaryAmount = scaledLongToMonetaryAmount("USD", amount)

        val descriptor = sale.customBilling?.descriptor
            ?: throw ProxiedRequestMissingParametersException("customBilling.descriptor")

        val authResponse = gatewayService.authorize(
            dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest(
                UUID.randomUUID().toString(),
                monetaryAmount,
                sale.customerId,
                sale.orderId,
                token,
                expMonth,
                expYear,
                billingInformation,
                null,
                true,
                descriptor,
                CreditCardNetwork.UNKNOWN
            )
        )

        return serialize(SaleResponse().apply {
            authCode = mapGatewayResponseToWorldPayResponseCode(authResponse)
            customerId = sale.customerId
            id = UUID.randomUUID().toString()
            litleTxnId = 0
            message = mapGatewayResponseToWorldPayMessage(authResponse)
            orderId = sale.orderId
            postDate =
                DatatypeFactory.newInstance().newXMLGregorianCalendar(authResponse.transactionDatetime.toString())
                    .toGregorianCalendar()
            reportGroup = sale.reportGroup
            response = mapGatewayResponseToWorldPayResponseCode(authResponse)
            responseTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
            approvedAmount = authResponse.amountAuthorized.toScaledLong()
        })
    }

    fun serialize(obj: SaleResponse): JAXBElement<SaleResponse> {
        return jaxbFactory.createSaleResponse(obj)
    }
}
