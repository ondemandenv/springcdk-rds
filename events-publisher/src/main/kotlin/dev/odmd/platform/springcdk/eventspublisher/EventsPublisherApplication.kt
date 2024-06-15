package dev.odmd.platform.springcdk.eventspublisher

import dev.odmd.platform.springcdk.domain.cryptography.TransitConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

private const val odmdBasePackage = "dev.odmd.platform.payments"

@EntityScan(odmdBasePackage)
@EnableJpaRepositories(odmdBasePackage)
@ConfigurationPropertiesScan(odmdBasePackage)
@ComponentScan(odmdBasePackage)
@SpringBootApplication
@EnableScheduling
@EnableRetry
@Import(TransitConfiguration::class)
class EventsPublisherApplication

fun main(args: Array<String>) {
	runApplication<EventsPublisherApplication>(*args)
}
