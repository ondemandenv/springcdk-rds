package dev.odmd.platform.springcdk.domain.repositories


import dev.odmd.platform.springcdk.domain.entities.PaymentTarget
import dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import javax.persistence.EntityNotFoundException

@Component
class PaymentTargetRepository internal constructor(
    private val targetJpaRepository: PaymentTargetJpaRepository,
    private val uniqueInsertTemplate: dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate
) {
    /**
     * Attempt to create a [PaymentTarget] or return one that already exists.
     */
    fun findOrCreatePaymentTarget(targetKey: String, targetType: String): dev.odmd.platform.springcdk.domain.entities.PaymentTarget {
        return createTargetIfUnique(
            targetKey = targetKey,
            targetType = targetType
        )
        ?: targetJpaRepository.findByTargetKeyAndTargetType(
            targetKey = targetKey,
            targetType = targetType
        )
        ?: throw IllegalStateException("Expected to find PaymentTarget type=$targetType & key=$targetKey after constraint violation.")
    }

    /**
     * Find PaymentTarget by type & key.
     *
     * Call this method if the PaymentTarget is expected to already exists. If the target needs to be created, use [findOrCreatePaymentTarget] instead.
     *
     * @return PaymentTarget The payment target with matching type and key.
     *
     * @throws PaymentTargetNotFoundException If no PaymentTarget exists.
     */
    fun getPaymentTarget(targetKey: String, targetType: String): dev.odmd.platform.springcdk.domain.entities.PaymentTarget {
        return targetJpaRepository.findByTargetKeyAndTargetType(
            targetKey = targetKey,
            targetType = targetType
        )
        ?: throw PaymentTargetNotFoundException(targetKey = targetKey, targetType = targetType)
    }

    private fun createTargetIfUnique(targetKey: String, targetType: String): dev.odmd.platform.springcdk.domain.entities.PaymentTarget? {
        return uniqueInsertTemplate.tryUniqueInsert(
            entity = dev.odmd.platform.springcdk.domain.entities.PaymentTarget(
                targetType = targetType,
                targetKey = targetKey
            ),
            uniqueConstraintName = "payment_targets_target_key_target_type_key"
        )
    }
}

/*
TODO: make JPA repository private in favor of PaymentTargetRepository
   */
@Repository
interface PaymentTargetJpaRepository : JpaRepository<dev.odmd.platform.springcdk.domain.entities.PaymentTarget, Long> {

    @EntityGraph(value = "target.payments")
    fun findByTargetKeyAndTargetType(targetKey: String, targetType: String): dev.odmd.platform.springcdk.domain.entities.PaymentTarget?
}

class PaymentTargetNotFoundException(val targetType: String, val targetKey: String)
    : EntityNotFoundException("Couldn't find PaymentTarget with targetKey=$targetKey targetType=$targetType")
