package dev.odmd.platform.springcdk.common

data class Address(
    val country: String,
    val lineOne: String,
    val lineTwo: String,
    val postalCode: String,
    val state: String,
    val city: String
) {
    override fun toString(): String {
        return "Address(country='$country', city='$city' postalCode='$postalCode', state='$state')"
    }
}
