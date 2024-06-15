package dev.odmd.platform.springcdk.eventspublisher.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryTemplateConfiguration {
    @Bean
    fun retryTemplate(retryConfig: RetryConfig) =
        RetryTemplate.builder()
            .infiniteRetry()
            .exponentialBackoff(
                retryConfig.initialInterval,
                retryConfig.multiplier,
                retryConfig.maxInterval
            )
            .retryOn(Exception::class.java)
            .build()
}
