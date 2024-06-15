package dev.odmd.platform.springcdk.domain.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "app.domain-events")
data class DomainEventConfig (val enabled: Boolean = false)