package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.CreditCardNumber
import dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest
import dev.odmd.platform.springcdk.gateways.Gateways
import dev.odmd.platform.springcdk.gateways.PaymentGatewaySupplier
import com.litle.sdk.generate.ObjectFactory
import com.litle.sdk.generate.RegisterTokenResponse
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory

@Service
internal class TokenizationStrategy(private val paymentGatewaySupplier: PaymentGatewaySupplier) : ProxyStrategy {
    override val operationName = "registerTokenRequest"
    private val accountNumberFieldName = "accountNumber"
    private val cvvFieldName = "cardValidationNum"
    private val jaxbFactory = ObjectFactory()

    override fun execute(
        value: JsonNode,
        entityId: String,
        defaultGateway: Gateways
    ): JAXBElement<RegisterTokenResponse> {
        val accountNumber = value.get(accountNumberFieldName)?.textValue()
            ?: throw ProxiedRequestMissingParametersException(accountNumberFieldName)
        val cvv = value.get(cvvFieldName)?.textValue()
        val token = paymentGatewaySupplier.get(defaultGateway).registerToken(
            dev.odmd.platform.springcdk.gateways.GatewayRegisterTokenRequest(
                UUID.randomUUID().toString(),
                CreditCardNumber(accountNumber.toCharArray()),
                cvv,
                0,
                0,
                entityId,
                null,
                null
            )
        )

        return serialize(RegisterTokenResponse().apply {
            litleTxnId = 0
            litleToken = token.token
            responseTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
            message = token.responseMessage
            response = mapGatewayRegisterTokenResponseToWorldPayResponse(token)
        })
    }

    fun serialize(obj: RegisterTokenResponse): JAXBElement<RegisterTokenResponse> {
        return jaxbFactory.createRegisterTokenResponse(obj)
    }
}
