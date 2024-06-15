package dev.odmd.platform.springcdk.authorization

import org.springframework.security.core.context.SecurityContextHolder

abstract class ResourceOwnerAuthorization {
    fun isOwner(resourceId: String?): Boolean {
        if (resourceId == null) return true // empty resources shouldn't be a risk

        val customerId = getCustomerIdByResourceId(resourceId)

        return isLoggedInUser(customerId)
    }

    fun ownsAll(resourceIds: List<String?>): Boolean = resourceIds.all { isOwner(it) }

    abstract fun getCustomerIdByResourceId(resourceId: String): String?

    private fun isLoggedInUser( customerId:String?) = customerId == SecurityContextHolder.getContext().authentication.name
}