package dev.odmd.platform.springcdk.gateways.worldpay.proxy

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.gateways.Gateways
import com.litle.sdk.generate.TransactionTypeWithReportGroup
import javax.xml.bind.JAXBElement

internal sealed interface ProxyStrategy {
    val operationName: String
    fun execute(
        value: JsonNode,
        entityId: String,
        defaultGateway: Gateways
    ): JAXBElement<out TransactionTypeWithReportGroup>
}
