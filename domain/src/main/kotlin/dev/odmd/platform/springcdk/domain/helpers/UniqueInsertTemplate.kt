package dev.odmd.platform.springcdk.domain.helpers

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.TransactionTemplate
import javax.persistence.EntityManager
import javax.persistence.PersistenceException

@Component
class UniqueInsertTemplate(
    private val transactionManager: PlatformTransactionManager,
    private val entityManager: EntityManager
) {
    /**
     * Attempt to insert an entity into the database, if it's unique.
     *
     * @param entity The entity to insert.
     *
     * @param uniqueConstraintName The name of the constraint which will fail if the entity is not unique.
     *
     * @return The entity, or null if insertion failed due to the unique constraint violation.
     *
     * @throws Exception Rethrows any exception that isn't the constraint violation.
     */
    fun <T> tryUniqueInsert(entity: T, uniqueConstraintName: String): T? =
        dev.odmd.platform.springcdk.domain.helpers.runCatchingConstraintViolation(uniqueConstraintName) {
            TransactionTemplate(transactionManager)
                .apply {
                    /*
                     this prevents the constraint violation from tainting the db session
                     which would cause errors when trying to run other queries after this
                     (i.e. "don't flush session after an exception occurs")
                     */
                    propagationBehavior = Propagation.REQUIRES_NEW.value()
                }
                .execute {
                    entityManager.persist(entity)
                }
            entity
        }.getOrThrow()
}

/**
 * Run the specified block, wrapping any errors in a Result.
 *
 * @return [Result] Which will either be the success value or null if a constraint violation occurred.
 * @throws Exception Anything thrown by the block or database errors (except the constraint violation).
 */
fun <T> runCatchingConstraintViolation(constraintName: String, block: () -> T): Result<T?> =
    runCatching(block).returnNullIfConstraintViolation(constraintName)

private fun <T> Result<T>.returnNullIfConstraintViolation(constraintName: String)
    = recoverCatching {
        if ((it is PersistenceException || it is DataIntegrityViolationException) &&
                it.isCausedByConstraintViolation(constraintName)) {
            null
        } else {
            throw it
        }
    }

private fun Throwable.isCausedByConstraintViolation(constraintName: String) =
    (cause as? ConstraintViolationException)?.constraintName == constraintName
