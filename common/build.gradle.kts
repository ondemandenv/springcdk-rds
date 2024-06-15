plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
    id("java-library")
    id("java-test-fixtures")
}

dependencies {
    api("org.javamoney:moneta:1.4.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.12")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")

    // logging
    implementation("ch.qos.logback.contrib:logback-jackson:0.1.5")
    implementation("ch.qos.logback.contrib:logback-json-classic:0.1.5")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-extension-annotations")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    val testContainersVersion = "1.16.3"
    testFixturesImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testFixturesImplementation("org.testcontainers:postgresql:$testContainersVersion")
}

description = "cdk-spring-rds-common"
