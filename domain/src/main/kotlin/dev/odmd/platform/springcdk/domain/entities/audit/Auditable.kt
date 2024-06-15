package dev.odmd.platform.springcdk.domain.entities.audit

import org.hibernate.annotations.Generated
import org.hibernate.annotations.GenerationTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import javax.persistence.Column
import javax.persistence.EntityListeners
import javax.persistence.MappedSuperclass

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
abstract class Auditable(
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    var createdBy: String = "",

    @Generated(value = GenerationTime.INSERT)
    @Column(name = "created_datetime", insertable = false, updatable = false)
    var createdDateTime: Instant = Instant.MIN,

    @LastModifiedBy
    @Column(name = "last_modified_by")
    var lastModifiedBy: String? = null,

    @LastModifiedDate
    @Column(name = "last_modified_datetime")
    var lastModifiedDateTime: Instant? = null
)
