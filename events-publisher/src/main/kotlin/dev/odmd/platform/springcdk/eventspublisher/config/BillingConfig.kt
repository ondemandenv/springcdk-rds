package dev.odmd.platform.springcdk.eventspublisher.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

@ConfigurationProperties(prefix = "app.billing")
@ConstructorBinding
data class BillingConfig(val url: URI, val apiKey: String)
