package dev.odmd.platform.springcdk.common

import io.swagger.v3.oas.annotations.media.Schema

data class BillingInformation(
    val name: String,
    val firstName: String,
    val lastName: String,
    @field:Schema(type = "string", example = "")
    val email: String,
    val phoneNumber: String,
    val address: dev.odmd.platform.springcdk.common.Address
) {
    override fun toString(): String {
        return "BillingInformation(address=$address)"
    }
}
