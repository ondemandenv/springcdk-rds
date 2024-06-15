package dev.odmd.platform.springcdk.dbmigration

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils


/**
 * Initializes the test database by enabling Flyway.
 *
 * Pair with [PostgresContainerInitializer] to simulate a production-like postgres database that has been fully migrated.
 *
 * This works because "testFixtures" implicitly includes the "main" source set, which will add both flyway and our migrations scripts to the classpath when tests are run.
 */
class DbMigrationInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            applicationContext,
            "spring.flyway.enabled=true"
        )
    }
}
