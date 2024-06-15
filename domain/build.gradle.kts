plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
    id("java-test-fixtures")
}

dependencies {
    implementation(project(":cdk-spring-rds-common"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    api("org.javamoney:moneta:1.4.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    val testContainersVersion = "1.16.3"
    testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
    testImplementation(testFixtures(project(":cdk-spring-rds-common")))
//    testImplementation(testFixtures(project(":cdk-spring-rds-db-migration")))

    testFixturesImplementation("org.springframework.boot:spring-boot-starter-test")
    testFixturesImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testFixturesImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

description = "cdk-spring-rds-domain"
