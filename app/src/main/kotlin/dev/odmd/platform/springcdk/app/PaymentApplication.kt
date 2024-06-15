package dev.odmd.platform.springcdk.app

import dev.odmd.platform.springcdk.domain.cryptography.TransitConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity

private const val odmdBasePackage = "dev.odmd.platform.payments"

@ComponentScan(dev.odmd.platform.springcdk.app.odmdBasePackage)
@EntityScan(dev.odmd.platform.springcdk.app.odmdBasePackage)
@EnableJpaRepositories(dev.odmd.platform.springcdk.app.odmdBasePackage)
@ConfigurationPropertiesScan(dev.odmd.platform.springcdk.app.odmdBasePackage)
@Import(TransitConfiguration::class)
@EnableGlobalMethodSecurity(prePostEnabled = true)
@SpringBootApplication
class PaymentApplication

fun main(args: Array<String>) {
    runApplication<dev.odmd.platform.springcdk.app.PaymentApplication>(*args)
}
