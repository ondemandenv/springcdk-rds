package dev.odmd.platform.springcdk.authorization

import org.springframework.stereotype.Service

@Service
class UserOwnerAuthorization(): ResourceOwnerAuthorization() {
    override fun getCustomerIdByResourceId(resourceId: String): String? {
        return resourceId
    }
}