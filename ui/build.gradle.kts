plugins {
    id("dev.odmd.platform.kotlin-application-conventions")
}

// Version compatible with Spring Boot 2.7.x
extra["springCloudAzureVersion"] = "4.6.0"

dependencies {
    implementation(project(":cdk-spring-rds-common"))
    implementation(project(":cdk-spring-rds-api"))
    implementation(project(":cdk-spring-rds-services"))
    implementation(project(":cdk-spring-rds-domain"))

    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.azure.spring:spring-cloud-azure-starter-active-directory")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation(testFixtures(project(":cdk-spring-rds-domain")))
}

dependencyManagement {
    imports {
        mavenBom("com.azure.spring:spring-cloud-azure-dependencies:${property("springCloudAzureVersion")}")
    }
}
