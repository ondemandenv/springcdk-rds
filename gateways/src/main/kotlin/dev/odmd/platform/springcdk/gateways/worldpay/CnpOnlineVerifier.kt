package dev.odmd.platform.springcdk.gateways.worldpay

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.MaskingMode
import dev.odmd.platform.springcdk.common.MockRequestResponse
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.common.mask
import io.github.vantiv.sdk.generate.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.xml.datatype.DatatypeFactory

class CnpOnlineVerifier(
    private val worldPayConfiguration: WorldPayConfiguration,
    private val mockRequestResponse: MockRequestResponse,
    private val logger: Logger = LoggerFactory.getLogger(CnpOnlineVerifier::class.java)
) : WorldPayClient {
    companion object {
        internal val datatypeFactory = DatatypeFactory.newInstance()
    }

    override fun registerToken(tokenRequest: RegisterTokenRequestType): RegisterTokenResponse {
        val methodName = "registerToken"
        val nestedNodeName = "registerTokenRequest"
        val registerTokenMockRequest = mockRequestResponse.expectedRequest.get(nestedNodeName)
        val expectedResponse = mockRequestResponse.expectedResponse

        verifyConfigurationProperties(methodName)

        verify(registerTokenMockRequest?.get("accountNumber")?.asText(), tokenRequest.accountNumber, methodName,"accountNumber", MaskingMode.LAST_FOUR)
        verify(registerTokenMockRequest?.get("customerId")?.asText(), tokenRequest.customerId, methodName,"customerId")

        return RegisterTokenResponse().apply {
            bin = expectedResponse.get("bin")?.asText()
            cnpToken = expectedResponse.get("litleToken")?.asText()
            cnpTxnId = expectedResponse.get("litleTxnId")?.asLong() ?: 0
            message = expectedResponse.get("message")?.asText()
            reportGroup =
                expectedResponse.get("reportGroup")?.asText()
            response = expectedResponse.get("response")?.asText()
            responseTime =
                expectedResponse
                    .get("responseTime")
                    ?.asText()
                    ?.let(datatypeFactory::newXMLGregorianCalendar)
            type =
                expectedResponse
                    .get("type")
                    ?.asText()
                    ?.let {
                        runCatching {
                            MethodOfPaymentTypeEnum.valueOf(it)
                        }.getOrNull()
                    }
                    ?: MethodOfPaymentTypeEnum.BLANK
        }
    }

    override fun sale(worldPaySaleRequest: Sale): SaleResponse {
        val methodName = "sale"
        val mockSaleRequest = mockRequestResponse.expectedRequest.get(methodName)
        val token = mockSaleRequest?.get("token")
        val expectedResponse = mockRequestResponse.expectedResponse

        verifyConfigurationProperties(methodName)

        verifyCommonAuthSaleRefundParams(
            mockSaleRequest,
            methodName,
            worldPaySaleRequest.customerId,
            worldPaySaleRequest.amount,
            worldPaySaleRequest.orderSource,
            worldPaySaleRequest.reportGroup,
            worldPaySaleRequest.enhancedData
        )

        verifyBillingInfo(mockSaleRequest?.get("billToAddress"), worldPaySaleRequest.billToAddress, "sale")

        verify(token?.get("litleToken")?.asText(), worldPaySaleRequest.token?.cnpToken, "sale", "token.litleToken", MaskingMode.ASTERISK)
        verify(token?.get("expDate")?.asText(), worldPaySaleRequest.token?.expDate, "sale", "token.expDate")

        return SaleResponse().apply {
            response = expectedResponse.get("response")?.asText()
            message = expectedResponse.get("message")?.asText()
            tokenResponse = TokenResponseType().apply {
                cnpToken = expectedResponse.get("litleToken")?.asText()
                bin = expectedResponse.get("bin")?.asText()
                tokenResponseCode = expectedResponse.get("tokenResponseCode")?.asText()
                tokenMessage = expectedResponse.get("tokenMessage")?.asText()
            }
            cnpTxnId = expectedResponse.get("litleTxnId")?.asLong() ?: 0
            orderId = expectedResponse.get("orderId")?.asText()
            customerId = expectedResponse.get("customerId")?.asText()
            reportGroup = expectedResponse.get("reportGroup")?.asText()
            responseTime = expectedResponse.get("responseTime")?.asText()?.let(datatypeFactory::newXMLGregorianCalendar)
            fraudResult  = FraudResult()
        }
    }

    override fun authorize(worldPayAuthorizeRequest: Authorization): AuthorizationResponse {
        val methodName = "authorize"
        val nestedNodeName = "authorization"
        val authorizationMockRequest = mockRequestResponse.expectedRequest.get(nestedNodeName)
        val expectedCard = authorizationMockRequest?.get("card")
        val expectedResponse = mockRequestResponse.expectedResponse

        verifyConfigurationProperties(methodName)

        verifyCommonAuthSaleRefundParams(authorizationMockRequest,methodName, worldPayAuthorizeRequest.customerId,
            worldPayAuthorizeRequest.amount, worldPayAuthorizeRequest.orderSource, worldPayAuthorizeRequest.reportGroup, worldPayAuthorizeRequest.enhancedData)

        verifyBillingInfo(authorizationMockRequest?.get("billToAddress"), worldPayAuthorizeRequest.billToAddress, methodName)

        val actualCard = worldPayAuthorizeRequest.card
        verify(expectedCard?.get("type")?.asText(), actualCard?.type?.value(), methodName,"card.type")
        verify(expectedCard?.get("number")?.asText(), actualCard?.number, methodName,"card.number", MaskingMode.LAST_FOUR)
        verify(expectedCard?.get("expDate")?.asText(), actualCard?.expDate, methodName,"card.expDate")

        return AuthorizationResponse().apply {
            reportGroup = expectedResponse.get("reportGroup")?.asText()
            response = expectedResponse.get("response")?.asText()
            message = expectedResponse.get("message")?.asText()
            tokenResponse = TokenResponseType().apply {
                cnpToken = expectedResponse.get("litleToken")?.asText()
                bin = expectedResponse.get("bin")?.asText()
                tokenResponseCode = expectedResponse.get("tokenResponseCode")?.asText()
                tokenMessage = expectedResponse.get("tokenMessage")?.asText()
            }
            cnpTxnId = expectedResponse.get("litleTxnId")?.asLong() ?: 0
            orderId = expectedResponse.get("orderId")?.asText()
            customerId = expectedResponse.get("customerId")?.asText()
            reportGroup = expectedResponse.get("reportGroup")?.asText()
            responseTime = expectedResponse.get("responseTime")?.asText()?.let(datatypeFactory::newXMLGregorianCalendar)
            fraudResult  = FraudResult()
        }
    }

    override fun credit(creditRequest: Credit): CreditResponse {
        val methodName = "Credit"
        val nestedNodeName = "credit"
        val creditMockRequest = mockRequestResponse.expectedRequest.get(nestedNodeName)
        val expectedResponse = mockRequestResponse.expectedResponse

        verifyConfigurationProperties(methodName)

        verifyCommonAuthSaleRefundParams(creditMockRequest, methodName, creditRequest.customerId, creditRequest.amount,
             creditRequest.orderSource, creditRequest.reportGroup, creditRequest.enhancedData)

        verify(creditMockRequest?.get("litleTxnId")?.asLong(), creditRequest.cnpTxnId, methodName, "cnpTxnId")

        return CreditResponse().apply {
            response = expectedResponse.get("response")?.asText()
            cnpTxnId = expectedResponse.get("litleTxnId")?.asLong() ?: 0
            message = expectedResponse.get("message")?.asText()
            tokenResponse = TokenResponseType().apply {
                cnpToken = expectedResponse.get("litleToken")?.asText()
                bin = expectedResponse.get("bin")?.asText()
                tokenResponseCode = expectedResponse.get("tokenResponseCode")?.asText()
                tokenMessage = expectedResponse.get("tokenMessage")?.asText()
            }
            customerId = expectedResponse.get("customerId")?.asText()
            reportGroup = expectedResponse.get("reportGroup")?.asText()
            responseTime = expectedResponse.get("responseTime")?.asText()?.let(datatypeFactory::newXMLGregorianCalendar)
            fraudResult  = FraudResult()
        }
    }

    fun <T> verify(
        expected: T?,
        actual: T?,
        methodName: String,
        fieldName: String,
        mask: MaskingMode? = null
    ) {

        if (expected != actual && !((expected?.toString().isNullOrEmpty() || expected.toString().trim() == "null")
                    && (actual?.toString().isNullOrEmpty() || actual.toString().trim() == "null"))
        ) {

            val expectedLogValue = mask?.let { expected?.mask(mask) } ?: expected.toString()
            val actualLogValue = mask?.let { actual?.mask(mask) } ?: actual.toString()

            logger.event(
                "worldpay.requestVerificationFailure",
                mapOf(
                    "requestVerificationExpected" to expectedLogValue,
                    "requestVerificationActual" to actualLogValue,
                    "requestVerificationMethodName" to methodName,
                    "requestVerificationFieldName" to fieldName
                )
            )
        }
    }

    private fun verifyConfigurationProperties(methodName: String) {
        val expectedMerchantId = mockRequestResponse.expectedRequest.get("merchantId")?.asText()
        verify(
            expectedMerchantId,
            worldPayConfiguration.merchantId,
            methodName,
            "merchantId"
        )
    }

    private fun verifyBillingInfo(billToAddress: JsonNode?, contact: Contact?, methodName: String) {
        verify(billToAddress?.get("state")?.asText(), contact?.state, methodName, "billToAddress.state")
        verify(billToAddress?.get("city")?.asText(), contact?.city, methodName, "billToAddress.city")
        verify(billToAddress?.get("name")?.asText(), contact?.name, methodName, "billToAddress.name", MaskingMode.ASTERISK)
        verify(billToAddress?.get("firstName")?.asText(), contact?.firstName, methodName, "billToAddress.firstName", MaskingMode.ASTERISK)
        verify(billToAddress?.get("lastName")?.asText(), contact?.lastName, methodName, "billToAddress.lastName", MaskingMode.ASTERISK)
        verify(billToAddress?.get("addressLine1")?.asText(), contact?.addressLine1, methodName, "billToAddress.addressLine1", MaskingMode.ASTERISK)
        verify(billToAddress?.get("addressLine2")?.asText(), contact?.addressLine2, methodName, "billToAddress.addressLine2", MaskingMode.ASTERISK)
        verify(billToAddress?.get("zip")?.asText(), contact?.zip, methodName, "billToAddress.zip", MaskingMode.ASTERISK)
        verify(billToAddress?.get("country")?.asText(), contact?.country?.value(), methodName, "billToAddress.country")
        verify(billToAddress?.get("email")?.asText(), contact?.email, methodName, "billToAddress.email", MaskingMode.ASTERISK)
        verify(billToAddress?.get("phone")?.asText(), contact?.phone, methodName, "billToAddress.phone", MaskingMode.ASTERISK)
    }

    private fun verifyCommonAuthSaleRefundParams(
        mockRequest: JsonNode?,
        methodName: String,
        customerId: String?,
        amount: Long?,
        orderSource: OrderSourceType?,
        reportGroup: String?,
        enhancedData: EnhancedData?
    ) {
        verify(mockRequest?.get("customerId")?.asText(), customerId, methodName, "customerId")
        verify(mockRequest?.get("amount")?.asText()?.toLong(), amount, methodName, "amount")
        verify(mockRequest?.get("orderSource")?.asText(), orderSource?.value(), methodName, "orderSource")
        verify(mockRequest?.get("reportGroup")?.asText(), reportGroup, methodName, "reportGroup")
        verify(mockRequest?.get("enhancedData")?.get("customerReference")?.asText(), enhancedData?.customerReference , methodName, "enhancedData.customerReference")
    }
}
