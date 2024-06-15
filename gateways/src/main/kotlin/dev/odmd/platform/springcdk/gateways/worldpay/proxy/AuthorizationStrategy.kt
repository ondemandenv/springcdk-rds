package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import dev.odmd.platform.springcdk.common.CreditCardNetwork
import dev.odmd.platform.springcdk.common.CreditCardNumber
import dev.odmd.platform.springcdk.common.scaledLongToMonetaryAmount
import dev.odmd.platform.springcdk.common.toScaledLong
import dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest
import dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest
import dev.odmd.platform.springcdk.gateways.Gateways
import dev.odmd.platform.springcdk.gateways.PaymentGatewaySupplier
import com.litle.sdk.generate.Authorization
import com.litle.sdk.generate.AuthorizationResponse
import com.litle.sdk.generate.ObjectFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory

@Service
internal class AuthorizationStrategy(private val paymentGatewaySupplier: PaymentGatewaySupplier) : ProxyStrategy {
    override val operationName = "authorization"
    private val xmlMapper = XmlMapper().findAndRegisterModules()
    private val jaxbFactory = ObjectFactory()

    private enum class AuthMethod {
        TOKEN,
        CARD
    }

    private data class AuthMethodInfo(
        val number: String,
        val expDate: String?,
        val authMethod: AuthMethod
    )

    override fun execute(
        value: JsonNode,
        entityId: String,
        defaultGateway: Gateways
    ): JAXBElement<AuthorizationResponse> {
        val authorization = xmlMapper.convertValue<Authorization>(value)

        val tokenNode = authorization.token
        val card = authorization.card

        val (number, expDate, authMethod) = if (tokenNode != null) {
            AuthMethodInfo(
                number = tokenNode.litleToken ?: throw ProxiedRequestMissingParametersException("token.litleToken"),
                expDate = tokenNode.expDate,
                authMethod = AuthMethod.TOKEN,
            )
        } else if (card != null) {
            AuthMethodInfo(
                number = card.number ?: throw ProxiedRequestMissingParametersException("card.number"),
                expDate = card.expDate,
                authMethod = AuthMethod.CARD,
            )
        } else {
            throw ProxiedRequestMissingParametersException("token OR card")
        }

        val (expMonth, expYear) = decodeExpDate(expDate)

        val gateway = when (authMethod) {
            AuthMethod.CARD -> defaultGateway
            AuthMethod.TOKEN -> decodeTokenGateway(number)
        }

        val amount = authorization.amount ?: throw ProxiedRequestMissingParametersException("amount")
        val monetaryAmount = scaledLongToMonetaryAmount("USD", amount)

        val descriptor = authorization.customBilling?.descriptor
            ?: throw ProxiedRequestMissingParametersException("customBilling.descriptor")

        val billToNode = authorization.billToAddress ?: throw ProxiedRequestMissingParametersException("billToAddress")
        val billingInformation = mapContactToBillingInformation(billToNode)

        val gatewayService = paymentGatewaySupplier.get(gateway)
        val authorizationResponse = when (authMethod) {
            AuthMethod.CARD -> gatewayService.authorize(
                dev.odmd.platform.springcdk.gateways.GatewayCreditCardAuthorizeRequest(
                    authorization.customerId,
                    UUID.randomUUID().toString(),
                    monetaryAmount,
                    authorization.orderId,
                    CreditCardNumber(number.toCharArray()),
                    expMonth,
                    expYear,
                    billingInformation,
                    null,
                    false,
                    descriptor
                )
            )

            AuthMethod.TOKEN -> gatewayService.authorize(
                dev.odmd.platform.springcdk.gateways.GatewayCreditCardTokenAuthorizeRequest(
                    UUID.randomUUID().toString(),
                    monetaryAmount,
                    authorization.customerId,
                    authorization.orderId,
                    number,
                    expMonth,
                    expYear,
                    billingInformation,
                    null,
                    false,
                    descriptor,
                    CreditCardNetwork.UNKNOWN
                )
            )
        }

        return serialize(AuthorizationResponse().apply {
            approvedAmount = authorizationResponse.amountAuthorized.toScaledLong()
            response = mapGatewayResponseToWorldPayResponseCode(authorizationResponse)
            responseTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
            message = mapGatewayResponseToWorldPayMessage(authorizationResponse)
        })
    }

    fun serialize(obj: AuthorizationResponse): JAXBElement<AuthorizationResponse> {
        return jaxbFactory.createAuthorizationResponse(obj)
    }
}

