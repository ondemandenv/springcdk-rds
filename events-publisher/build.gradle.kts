
plugins {
	id("dev.odmd.platform.kotlin-application-conventions")
	application
}

dependencies {
	implementation(project(":cdk-spring-rds-common"))
	implementation(project(":cdk-spring-rds-domain"))

	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.retry:spring-retry:2.0.1")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.postgresql:postgresql")

	testImplementation(testFixtures(project(":cdk-spring-rds-common")))
	testImplementation(testFixtures(project(":cdk-spring-rds-domain")))
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	val testContainersVersion = "1.16.3"
	testImplementation("org.testcontainers:testcontainers:$testContainersVersion")
}
description = "cdk-spring-rds-events-publisher"

application {
	// Define the main class for the application.
	mainClass.set("dev.odmd.platform.cdk-spring-rds.eventspublisher.EventsPublisherApplicationKt")
}
