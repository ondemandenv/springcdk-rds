package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import org.hibernate.annotations.Type
import javax.persistence.*


@Table(name = "payment_transaction_metadata")
@Entity
class PaymentTransactionMetadata(
    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var metadata: JsonNode,

    @field:OneToOne(fetch = FetchType.LAZY, optional = false)
    var paymentTransaction: PaymentTransaction
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
