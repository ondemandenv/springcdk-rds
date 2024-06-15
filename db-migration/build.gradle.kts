plugins {
    id("dev.odmd.platform.kotlin-application-conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    // We don't use JPA directly in this application, but we need this
    // to get Spring autoconf to run Flyway automatically
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation(project(":cdk-spring-rds-common"))

    testImplementation(project(":cdk-spring-rds-domain"))
    testImplementation(kotlin("reflect"))

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    val testContainersVersion = "1.16.3"
    testFixturesImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testFixturesImplementation("org.testcontainers:postgresql:$testContainersVersion")
}

description = "cdk-spring-rds-db-migration"
