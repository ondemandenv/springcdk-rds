package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import java.math.BigDecimal
import javax.persistence.*

@Table(name = "payment_transactions_line_items")
@Entity
class PaymentTransactionLineItem(
    var currencyAmountApplied: BigDecimal,

    @field:ManyToOne(optional = false, cascade = [CascadeType.PERSIST, CascadeType.MERGE])
    var paymentTransaction: PaymentTransaction,

    @field:ManyToOne(optional = false)
    var lineItem: dev.odmd.platform.springcdk.domain.entities.LineItem
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
