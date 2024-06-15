package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import java.math.BigDecimal
import java.time.Instant
import javax.persistence.*

@Table(name = "payments")
@Entity
@NamedEntityGraph(
    name = "payments.transactions",
    attributeNodes = [
        NamedAttributeNode(value = "paymentTarget"),
        NamedAttributeNode(value = "paymentMetadata"),
        NamedAttributeNode(value = "paymentTransactions", subgraph = "subgraph.paymentTransaction")],
    subgraphs = [
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
class Payment(
    var externalPaymentId: String,

    @field:Column(name = "payment_datetime")
    var paymentDateTime: Instant,

    @field:Column(name = "stopped_datetime")
    var stoppedDateTime: Instant?,

    var requestedBy: String,

    var currency: String,

    var currencyAmount: BigDecimal,
    /**
     * Direct relationships
     */
    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    var paymentTarget: dev.odmd.platform.springcdk.domain.entities.PaymentTarget,

    @field:OneToOne(mappedBy = "payment", fetch = FetchType.LAZY, cascade = [CascadeType.PERSIST])
    var paymentMetadata: dev.odmd.platform.springcdk.domain.entities.PaymentMetadata?,

    /**
     * paymentTransactions
     *
     * The (ordered) set of [PaymentTransaction] entities belonging to this [Payment].
     *
     * [LinkedHashSet] is used to work around Hibernate (intentionally but unhelpfully)
     * inserting duplicates into this list when populated via [FetchType.LAZY].
     *
     * For more information, read [Hibernate's documentation of outer joins](https://developer.jboss.org/docs/DOC-15782#jive_content_id_Hibernate_does_not_return_distinct_results_for_a_query_with_outer_join_fetching_enabled_for_a_collection_even_if_I_use_the_distinct_keyword).
     */
    @field:OneToMany(
        mappedBy = "payment",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE]
    )
    @OrderBy(value = "created_datetime asc")
    var paymentTransactions: MutableSet<PaymentTransaction> = LinkedHashSet()
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
