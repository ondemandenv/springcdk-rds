package dev.odmd.platform.springcdk.dbmigration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class PostgresContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    private val postgresContainer = PostgreSQLContainer<Nothing>("postgres:13.4").apply {
        withDatabaseName("payments_platform")
        withUsername("payments_platform")
        withPassword("payments_platform")
        withTmpFs(mapOf("/var/lib/postgresql/data" to "rw"))
    }

    override fun initialize(configurableApplicationContext: ConfigurableApplicationContext) {
        postgresContainer.start()

        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            configurableApplicationContext,
            "spring.datasource.url=" + postgresContainer.jdbcUrl,
            "spring.datasource.username=payments_platform",
            "spring.datasource.password=payments_platform"
        )
    }
}
