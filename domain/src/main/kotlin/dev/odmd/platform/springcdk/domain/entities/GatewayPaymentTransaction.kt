package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import org.hibernate.annotations.Type
import javax.persistence.*

public enum class GatewayTransactionType {
    AUTH,AUTH_CAPTURE,CAPTURE,REFUND
}

@Table(name = "gateway_payment_transactions")
@Entity
class GatewayPaymentTransaction(
    @Enumerated(EnumType.STRING)
    var gatewayTransactionType: GatewayTransactionType,

    // TODO: replace with enum
    var status: String,

    gatewayRequest: Any,

    gatewayResponse: Any,

    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    var paymentTransaction: PaymentTransaction,

    var gatewayIdentifier: String,

) : Auditable() {
    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var gatewayRequest: JsonNode

    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var gatewayResponse: JsonNode

    init {
        val objectMapper = ObjectMapper().findAndRegisterModules()
        this.gatewayRequest = objectMapper.valueToTree(gatewayRequest)
        this.gatewayResponse = objectMapper.valueToTree(gatewayResponse)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
