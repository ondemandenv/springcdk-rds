plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
}

dependencies {
    implementation(project(":cdk-spring-rds-common"))
    implementation("org.javamoney:moneta:1.4.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    implementation("com.stripe:stripe-java:20.120.0")
    implementation("io.github.vantiv", "cnp-sdk-for-java", "12.25.0")
    implementation("com.litle:litle-sdk-for-java:8.31.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.2")

    testImplementation("io.mockk:mockk:1.13.2")
}

description = "cdk-spring-rds-gateways"
