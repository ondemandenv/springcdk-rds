package dev.odmd.platform.springcdk.dbmigration

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [DbMigrationApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = [
    PostgresContainerInitializer::class
])
@TestPropertySource(properties = [
    "spring.jpa.hibernate.ddl-auto=validate"
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EntityScan(value = ["dev.odmd.platform.springcdk.domain.entities"])
class DbMigrationApplicationTest {
    @Test
    fun whenMigrationsAreRun_thenSchemasAreValid() {
        // flyway migrations & validations are run by Spring automatically
    }
}
