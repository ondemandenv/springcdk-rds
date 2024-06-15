plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
}

dependencies {
    implementation(project(":cdk-spring-rds-gateways"))
    implementation(project(":cdk-spring-rds-domain"))
    implementation(project(":cdk-spring-rds-common"))
    implementation(project(":cdk-spring-rds-api"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation(testFixtures(project(":cdk-spring-rds-common")))
    testImplementation(testFixtures(project(":cdk-spring-rds-domain")))
    testImplementation(testFixtures(project(":cdk-spring-rds-db-migration")))
}
