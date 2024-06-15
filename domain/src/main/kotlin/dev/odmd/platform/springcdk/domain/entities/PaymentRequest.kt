package dev.odmd.platform.springcdk.domain.entities

import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table


@Table(name = "requests")
@Entity
class PaymentRequest(
    @Id val uuid: String
) {
    @Generated(value = GenerationTime.INSERT)
    @Column(name = "created_at", insertable = false, updatable = false)
    lateinit var createdAt: Instant

    @Column(nullable = true)
    var lockedAt: Instant? = null

    @Column(nullable = true)
    var performedAt: Instant? = null

    var affectedEntityId: Long? = null
}
