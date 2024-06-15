package dev.odmd.platform.springcdk.domain.repositories

import dev.odmd.platform.springcdk.domain.entities.LineItem
import dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate
import dev.odmd.platform.springcdk.common.event
import org.slf4j.LoggerFactory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import javax.persistence.EntityNotFoundException

@Repository
interface LineItemJpaRepository : JpaRepository<LineItem, Long> {
    fun findAllByExternalLineItemIdIn(externalLineItemIds: Set<String>): Set<LineItem>

    fun findByExternalLineItemId(externalLineItemId: String): LineItem?
}

@Component
class LineItemRepository internal constructor(
    private val lineItemJpaRepository: LineItemJpaRepository,
    private val uniqueInsertTemplate: dev.odmd.platform.springcdk.domain.helpers.UniqueInsertTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(LineItemRepository::class.java)
    }

    fun findOrCreateLineItem(
        externalLineItemId: String,
        currencyAmount: BigDecimal,
        description: String
    ): LineItem {
        return createIfUnique(
            externalLineItemId = externalLineItemId,
            currencyAmount = currencyAmount,
            description = description
        )
        ?: findLineItem(
            externalLineItemId = externalLineItemId,
            currencyAmount = currencyAmount,
            description = description
        )
        ?: throw IllegalStateException("Expected to find line item $externalLineItemId after unique constraint violation.")
    }

    fun getAllByExternalLineItemIdIn(externalLineItemIds: Set<String>): Set<LineItem> {
        return lineItemJpaRepository
            .findAllByExternalLineItemIdIn(externalLineItemIds)
            .also { lineItems ->
                val foundLineItemIds = lineItems.map { it.externalLineItemId }.toSet()
                val missingLineItemIds = externalLineItemIds - foundLineItemIds
                if (missingLineItemIds.isNotEmpty()) {
                    throw MultipleLinesItemNotFoundException(missingLineItemIds)
                }
            }
    }

    private fun findLineItem(
        externalLineItemId: String,
        currencyAmount: BigDecimal,
        description: String
    ): LineItem? {
        return lineItemJpaRepository.findByExternalLineItemId(externalLineItemId)?.also {
            if (it.currencyAmount != currencyAmount) {
                logExistingFieldMismatch(
                    externalLineItemId = externalLineItemId,
                    fieldName = "currencyAmount",
                    existingFieldValue = it.currencyAmount,
                    requestedFieldValue = currencyAmount
                )
            }
            if (it.description != description) {
                logExistingFieldMismatch(
                    externalLineItemId = externalLineItemId,
                    fieldName = "description",
                    existingFieldValue = it.description,
                    requestedFieldValue = description
                )
            }
        }
    }

    private fun createIfUnique(
        externalLineItemId: String,
        currencyAmount: BigDecimal,
        description: String
    ): LineItem? {
        val lineItem = LineItem(
            externalLineItemId = externalLineItemId,
            currencyAmount = currencyAmount,
            description = description
        )
        return uniqueInsertTemplate.tryUniqueInsert(
            entity = lineItem,
            // see constraint name specified in V14 migration
            uniqueConstraintName =  "line_items_external_line_item_id_key"
        )
    }

    private fun logExistingFieldMismatch(
        externalLineItemId: String,
        fieldName: String,
        existingFieldValue: Any,
        requestedFieldValue: Any
    ) {
        logger.event(
            eventName = "lineItem.findOrCreate.paramMismatch",
            eventData = mapOf(
                "lineItem.findOrCreate.paramMismatch" to mapOf(
                    "fieldName" to fieldName,
                    "externalLineItemId" to externalLineItemId,
                    "$fieldName.existing" to existingFieldValue,
                    "$fieldName.requested" to requestedFieldValue
                )
            )
        )
    }
}

class MultipleLinesItemNotFoundException(val externalLineItemIds: Set<String>)
    : EntityNotFoundException("No LineItems found with externalLineItemId in $externalLineItemIds")
