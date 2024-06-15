plugins {
    id("dev.odmd.platform.kotlin-application-conventions")
    application
}

dependencies {
    implementation(project(":cdk-spring-rds-api"))
    implementation(project(":cdk-spring-rds-domain"))
    implementation(project(":cdk-spring-rds-gateways"))
    implementation(project(":cdk-spring-rds-common"))
    implementation(project(":cdk-spring-rds-webhooks"))
    implementation(project(":cdk-spring-rds-services"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-web")
    /** Swagger **/
    implementation("org.springdoc:springdoc-openapi-ui:1.6.12")

    testImplementation(testFixtures(project(":cdk-spring-rds-common")))
    testImplementation(testFixtures(project(":cdk-spring-rds-domain")))
    testImplementation(testFixtures(project(":cdk-spring-rds-db-migration")))
    testImplementation(project(":cdk-spring-rds-services"))

    ///auth0 integration.
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.springframework.security:spring-security-config")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")
}
description = "payment-app"

application {
    mainClass.set("dev.odmd.platform.springcdk.app.PaymentApplicationKt")
}
