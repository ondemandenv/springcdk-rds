package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import javax.persistence.*

@Table(name = "payment_targets")
@Entity
@NamedEntityGraph(
    name = "target.payments",
    attributeNodes = [
        NamedAttributeNode(value = "payments", subgraph = "subgraph.payments.transactions")],
    subgraphs = [
        NamedSubgraph(
            name = "subgraph.payments.transactions",
            attributeNodes = [
                NamedAttributeNode(value = "paymentMetadata"),
                NamedAttributeNode(value = "paymentTransactions", subgraph = "subgraph.paymentTransaction")
            ]
        ),
        NamedSubgraph(
            name = "subgraph.paymentTransaction",
            attributeNodes = [NamedAttributeNode(value = "paymentProfile"),
                NamedAttributeNode(value = "gatewayPaymentTransactions"),
                NamedAttributeNode(value = "lineItems", subgraph = "subgraph.lineItem"),
                NamedAttributeNode(value = "paymentTransactionMetadata")
            ]
        ),
        NamedSubgraph(
            name = "subgraph.lineItem",
            attributeNodes = [NamedAttributeNode(value = "lineItem")]
        )
    ]
)

class PaymentTarget(
    var targetType: String,

    var targetKey: String,

    @field:OneToMany(fetch = FetchType.LAZY, mappedBy = "paymentTarget")
    var payments: MutableSet<dev.odmd.platform.springcdk.domain.entities.Payment> = mutableSetOf()
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
