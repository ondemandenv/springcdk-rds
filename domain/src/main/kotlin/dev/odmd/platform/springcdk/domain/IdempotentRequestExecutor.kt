package dev.odmd.platform.springcdk.domain

//import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.domain.entities.PaymentRequest
import dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate
import dev.odmd.platform.springcdk.domain.repositories.RequestRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.dao.CannotAcquireLockException
import org.springframework.http.HttpStatus
import org.springframework.orm.jpa.JpaSystemException
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ
import org.springframework.transaction.TransactionException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.bind.annotation.ResponseStatus
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManager

@Component
class IdempotentRequestExecutor @Autowired constructor(
    val requestRepository: dev.odmd.platform.springcdk.domain.repositories.RequestRepository,
    val entityManager: EntityManager,
    val transactionManager: PlatformTransactionManager,
    val config: IdempotentRequestConfig
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IdempotentRequestExecutor::class.java)
    }

    /**
     * Run [action] only if it hasn't been called before with the same [requestId].
     *
     * @param requestId Used to identify whether the action has been performed. Passing the same value across multiple attempts will ensure [action] is only called once.
     * @param action    Lambda which will only be called once for a given [requestId].
     * @param alreadyRun  Lambda which will only be called if the idempotency check failed.
     *
     * @return [Unit] This is intended to run requests with no consumable result.
     */
    fun runOnce(
        requestId: String,
        alreadyRun: (() -> Unit) = {},
        action: () -> Unit,
    ): Unit = performOnce(requestId,
        alreadyPerformed = { alreadyRun() }
    ) {
        action()
        0L to Unit
    }

    /**
     * Call [action] only if it hasn't been called before with the same [requestId].
     *
     * @param requestId Used to identify whether the action has been performed. Passing the same value across multiple attempts will ensure [action] is only called once.
     * @param action    Lambda which will only be called once for a given [requestId].
     * @param alreadyPerformed  Lambda which will only be called if the idempotency check failed.
     *
     * @return The value returned by [action] if the idempotency check passed, or [alreadyPerformed] if it failed.
     */
    fun <T> performOnce(
        requestId: String,
        alreadyPerformed: (Long) -> T,
        action: () -> Pair<Long, T>
    ): T = MDC.putCloseable("idempotentRequestId", requestId).use {
        logger.info("Attempting idempotent request")

        try {
            val paymentRequest = getOrCreateAndLockRequest(requestId)

            paymentRequest
                .affectedEntityId?.let(alreadyPerformed)
                ?: performRequest(paymentRequest, action)
        } catch (e: CannotAcquireLockException) {
            // if any atomic phase fails, wrap exception with request ID
            logger.warn("Idempotent request failed due to database error", e)
            throw ConcurrentRequestException(requestId, e)
        } catch (e: JpaSystemException) {
            if (e.cause is TransactionException) {
                logger.warn("Idempotent request failed due to database error", e)
                throw ConcurrentRequestException(requestId, e)
            } else {
                throw e
            }
        }
    }

    private fun <T> atomically(f: () -> T): T =
        TransactionTemplate(transactionManager)
            .apply {
                /*
                This was originally set to SERIALIZABLE, but we ran into too many issues with serialization
                 failures. These failures might have been due to postgres using sequential table scans
                 when the tables were small, which manifested as serializable errors that would (eventually)
                 resolve on their own after running tests a few times (to populate tables & cause
                 postgres to switch to using indexes).
                 */
                isolationLevel = ISOLATION_REPEATABLE_READ
                propagationBehavior = Propagation.REQUIRES_NEW.value()
            }
            .execute {
                f()
            }!!

    private fun <T> performRequest(paymentRequest: dev.odmd.platform.springcdk.domain.entities.PaymentRequest, action: () -> Pair<Long, T>): T =
        try {
            atomically {
                val (entityId, result) = action()
                paymentRequest.unlock(entityId)
                result
            }
        } catch (e: Exception) {
            paymentRequest.handleExecutionFailure(e)
            throw e
        }

    private fun dev.odmd.platform.springcdk.domain.entities.PaymentRequest.unlock(affectedEntityId: Long?): dev.odmd.platform.springcdk.domain.entities.PaymentRequest {
        this.affectedEntityId = affectedEntityId
        // only set performedAt if there is an affectedEntityId
        performedAt = affectedEntityId?.let { Instant.now() }
        lockedAt = null
        return requestRepository.save(this)
    }

    private fun dev.odmd.platform.springcdk.domain.entities.PaymentRequest.handleExecutionFailure(throwable: Throwable) {
        try {
            logger.info("Unlocking request after action error ${throwable.message}")
            val unlockedResult = unlock(null)
            logger.info("unlocked $unlockedResult")
        } catch (error: Exception) {
            logger.warn("Unable to defensively unlock request: ${error.message}")
        }
    }

    /**
     * Obtain a request with a [uuid] equal to [requestId], creating or updating/locking as necessary.
     *
     * @return The request, which will only be locked if it hasn't been performed.
     */
    private fun getOrCreateAndLockRequest(requestId: String): dev.odmd.platform.springcdk.domain.entities.PaymentRequest =
        createRequestIfUnique(requestId)
            ?: lockRequestIfNeeded(requestId)

    /**
     * @return [PaymentRequest] that was created with the specified [requestId], or null if it already exists.
     *
     * This is *not* done within a serializable transaction since the unique constraint of the request uuid
     * column will guarantee that only one process commits it successfully. Using a serializable
     * transaction would result in a large number of errors when requests are created in parallel,
     * even if their uuids are different.
     */
    private fun createRequestIfUnique(requestId: String): dev.odmd.platform.springcdk.domain.entities.PaymentRequest? {
        val paymentRequest = dev.odmd.platform.springcdk.domain.entities.PaymentRequest(uuid = requestId).apply {
            lockedAt = Instant.now()
        }
        return dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate(
            transactionManager = transactionManager,
            entityManager = entityManager
        ).tryUniqueInsert(entity = paymentRequest, uniqueConstraintName = "requests_pkey")
    }

    /**
     * Get and lock the request with the specified [requestId].
     *
     * This is done in a serializable transaction to prevent other concurrent requests
     * from updating (i.e. locking) the same request.
     *
     * Assumes the request already exists.
     */
    private fun lockRequestIfNeeded(requestId: String): dev.odmd.platform.springcdk.domain.entities.PaymentRequest =
        atomically {
            requestRepository
                .findById(requestId)
                .map { paymentRequest ->
                    if (paymentRequest.affectedEntityId == null) {
                        val lockedAt = paymentRequest.lockedAt
                        if (lockedAt == null || config.isExpired(lockedAt)) {
//                            logger.event("Locking request", mapOf("lastLockedAt" to lockedAt))
                            paymentRequest.lockedAt = Instant.now()
                        } else {
                            throw RequestLockedException(
                                requestId = requestId,
                                lockedAt = lockedAt,
                                expiredAt = config.expirationOf(lockedAt)
                            ).also {
                                logger.warn(it.message)
                            }
                        }
                        requestRepository.save(paymentRequest)
                    } else {
                        logger.debug("Skipping lock since request was already performed")
                    }
                    paymentRequest
                }
                .orElseThrow {
                    IllegalStateException("Request $requestId should exist since creation failed due to primary key constraint.")
                }
        }
}


@ConfigurationProperties(prefix = "app.idempotency")
@ConstructorBinding
data class IdempotentRequestConfig(
    val lockTimeout: Duration = Duration.of(10, ChronoUnit.SECONDS)
) {
    fun isExpired(lockedAt: Instant): Boolean = expirationOf(lockedAt).isBefore(Instant.now())

    fun expirationOf(lockedAt: Instant): Instant = lockedAt.plus(lockTimeout)
}

@ResponseStatus(HttpStatus.LOCKED)
class RequestLockedException(
    val requestId: String,
    val lockedAt: Instant,
    val expiredAt: Instant
) : RuntimeException("Unable to perform request $requestId because it was locked at $lockedAt. Retries will be allowed after lock is reset by another process or it expires after $expiredAt.")

@ResponseStatus(HttpStatus.CONFLICT)
class ConcurrentRequestException(
    val requestId: String,
    cause: Throwable
) : RuntimeException(
    "Parallel request with ID $requestId rejected because another process is performing it concurrently.",
    cause
)
