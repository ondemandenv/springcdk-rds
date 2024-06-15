plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
}

dependencies {
//    implementation(project(":cdk-spring-rds-gateways"))
    implementation(project(":cdk-spring-rds-domain"))
    implementation(project(":cdk-spring-rds-common"))
    implementation(project(":cdk-spring-rds-services"))
    implementation(project(":cdk-spring-rds-api"))
    testImplementation(testFixtures(project(":cdk-spring-rds-domain")))
    testImplementation(testFixtures(project(":cdk-spring-rds-db-migration")))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("com.google.code.gson:gson:2.9.1")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springdoc:springdoc-openapi-ui:1.6.12")
    implementation("com.stripe:stripe-java:20.120.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.mockk:mockk:1.13.2")
}
