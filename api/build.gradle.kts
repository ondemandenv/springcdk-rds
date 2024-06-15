plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
}

dependencies {
    implementation(project(":cdk-spring-rds-common"))
    compileOnly("org.springframework.boot:spring-boot-starter-web")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation")


    /** Swagger **/
    implementation("org.springdoc:springdoc-openapi-ui:1.6.12")
}
