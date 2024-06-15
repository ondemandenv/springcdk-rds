plugins {
    id("com.gorylenko.gradle-git-properties")
    id("dev.odmd.platform.kotlin-common-conventions")
    id("jacoco")
}
springBoot { buildInfo() }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-kubernetes-client-config")
    testImplementation("org.jacoco:org.jacoco.agent:0.8.2")
    testImplementation("org.jacoco:org.jacoco.cli:0.8.5")
}