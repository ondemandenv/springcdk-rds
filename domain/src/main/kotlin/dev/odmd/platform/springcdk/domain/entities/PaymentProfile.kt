package dev.odmd.platform.springcdk.domain.entities

import com.fasterxml.jackson.databind.JsonNode
import dev.odmd.platform.springcdk.common.ProfilePaymentType
import dev.odmd.platform.springcdk.domain.entities.audit.Auditable
import com.vladmihalcea.hibernate.type.json.JsonType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import javax.persistence.*

@Table(name = "payment_profiles")
@Entity
// This typedef only needs to be declared once, and can be reused on other entities
@TypeDef(name = "jsonb", typeClass = JsonType::class)
class PaymentProfile(
    var externalId: String,

    var isDefault: Boolean,

    var isReusable: Boolean,

    var isActive: Boolean,

    var customerId: String,

    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var methodInformation: dev.odmd.platform.springcdk.domain.entities.PaymentMethodInformation,

    var gatewayIdentifier: String,

    @Enumerated(EnumType.STRING)
    var profilePaymentType: ProfilePaymentType,

    @field:Type(type = "jsonb")
    @field:Column(columnDefinition = "jsonb")
    var metadata: JsonNode?  = null

    ) : Auditable() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
}
