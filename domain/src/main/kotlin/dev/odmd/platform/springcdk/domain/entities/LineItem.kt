package dev.odmd.platform.springcdk.domain.entities

import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import java.math.BigDecimal
import javax.persistence.*

@Table(name = "line_items")
@Entity
class LineItem(
    var externalLineItemId: String,

    var description: String,

    var currencyAmount: BigDecimal
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
