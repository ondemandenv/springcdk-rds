package dev.odmd.platform.springcdk.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "auth0")
data class SecurityProperties(
    val tenants: List<TenantProperties>
)

data class TenantProperties(
    val audience: String,
    val issuerUri: String
)