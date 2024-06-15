package dev.odmd.platform.springcdk.eventspublisher.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.retry.backoff.ExponentialBackOffPolicy

@ConfigurationProperties(prefix = "app.retry-backoff")
@ConstructorBinding
data class RetryConfig(
    // Initial retry delay in milliseconds
    val initialInterval: Long = ExponentialBackOffPolicy.DEFAULT_INITIAL_INTERVAL,

    // How much the delay is multiplied for each retry
    val multiplier: Double = ExponentialBackOffPolicy.DEFAULT_MULTIPLIER,

    // The maximum delay between retries
    val maxInterval: Long = ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL
)
