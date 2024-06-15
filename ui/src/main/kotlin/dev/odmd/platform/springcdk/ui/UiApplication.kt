package dev.odmd.platform.springcdk.ui

import dev.odmd.platform.springcdk.domain.cryptography.TransitConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

private const val odmdBasePackage = "dev.odmd.platform.payments"

@ComponentScan(odmdBasePackage)
@EntityScan(odmdBasePackage)
@EnableJpaRepositories(odmdBasePackage)
@ConfigurationPropertiesScan(odmdBasePackage)
@Import(TransitConfiguration::class)
@SpringBootApplication
class UiApplication

fun main(args: Array<String>) {
    runApplication<UiApplication>(*args)
}
