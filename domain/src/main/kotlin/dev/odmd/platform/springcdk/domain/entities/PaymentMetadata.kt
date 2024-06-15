package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import org.hibernate.annotations.Type
import javax.persistence.*

@Table
@Entity
class PaymentMetadata(
    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var metadata: JsonNode,

    @field:OneToOne(fetch = FetchType.LAZY, optional = false)
    var payment: dev.odmd.platform.springcdk.domain.entities.Payment
) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
