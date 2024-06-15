package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.common.TransactionStatus
import dev.odmd.platform.springcdk.common.TransactionType
import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import org.javamoney.moneta.FastMoney
import java.math.BigDecimal
import javax.money.MonetaryAmount
import javax.persistence.*


@Table(name = "payment_transactions")
@Entity
class PaymentTransaction(
    var externalPaymentTransactionId: String,

    @Enumerated(EnumType.STRING)
    var paymentTransactionType: TransactionType,

    var source: String,

    var currencyAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    var transactionStatus: TransactionStatus,

    /**
     * Direct relationships
     */
    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    var payment: Payment,

    @field:ManyToOne(fetch = FetchType.LAZY, optional = false)
    var paymentProfile: PaymentProfile,

    var reason: String,

    @field:OneToMany(
        mappedBy = "paymentTransaction",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
    )
    var gatewayPaymentTransactions: MutableSet<GatewayPaymentTransaction> = mutableSetOf(),


    @field:OneToMany(
        mappedBy = "paymentTransaction",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
    )
    var lineItems: MutableSet<PaymentTransactionLineItem> = mutableSetOf(),

    @field:OneToOne(
        mappedBy = "paymentTransaction",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE]
    )
    var paymentTransactionMetadata: PaymentTransactionMetadata?
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}

val PaymentTransaction.monetaryAmount: MonetaryAmount
    get() = FastMoney.of(currencyAmount, payment.currency)
