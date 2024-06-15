import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// language versions
val javaTargetVersion = "17"
val javaCompatibilityVersion = "11"


val springBootVersion = "2.7.7"
/** Slf4j **/
val versionSlf4jLog4j = "2.16.0"

/** Persistence **/
val versionJavaxPersistence = "2.2"
val versionSpringEnvers = "2.5.4"

/** Override spring hibernate version **/
val hibernateVersion = "5.4.8.Final"
val versionHibernateEm = "5.2.7.Final"

/** Test **/
val versionMavenJarPlugin = "3.2.0"
val versionH2db = "1.4.200"

/** Apache commons **/
val versionApacheCommons = "2.10.0"
val apacheMavenWarPluginVersion = "2.1.1"

/** Metrics logging **/
val versionMicrometerLogging = "1.9.6"
/** Javax rs **/
val versionJavaxRs = "2.1.1"
val versionJavaxValidation = "2.0.1.Final"

/** Retrofit for api cals **/
val versionRetrofit = "2.9.0"
val versionGsonConverterRetrofit = "2.3.0"

/** okhttp3 **/
val versionSquareupOkhttp3Okhttp = "3.14.9"
val versionSquareupOkhttp2Mockwebserver = "3.14.9"


plugins {
    id("jacoco")
    id("org.openapi.generator")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("project-report")
    id("maven-publish")
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")
    kotlin("plugin.spring")
}

group = "dev.odmd.platform"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_17
}

configurations {
    all {
        exclude(group = "commons-logging")
        exclude(group = "javax.activation")
        exclude(group = "javax.mail")
        exclude(group = "javax.persistence")
        exclude(group = "javax.servlet")
        exclude(group = "javax.xml.bind")
        exclude(group = "org.aspectj", module = "aspectjrt")
        exclude(group = "org.jboss.spec.javax.transaction")
    }
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    jcenter()
    /*
    maven {
        url = uri("https://artifactory.ondemandenv.dev/artifactory/public")
    }*/

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2021.0.1"))
    implementation(platform("org.springframework.cloud:spring-cloud-sleuth-otel-dependencies:1.1.0"))
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:1.0.8")

//    implementation(platform("io.awspring.cloud:spring-cloud-aws-dependencies"))

    /** Hibernate envers**/
    implementation("org.springframework.data:spring-data-envers")

    implementation("io.micrometer:micrometer-registry-prometheus:$versionMicrometerLogging")

    /** Mockito core for getting mockito **/
    val mockitoVersion = "4.0.0"
    testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoVersion")
    // fixes "cannot mock/spy because - final class"
    testImplementation("org.mockito:mockito-inline:$mockitoVersion")

    testImplementation("com.h2database:h2:$versionH2db")

//    testImplementation(kotlin("test"))
    /** Javax RS **/
    implementation("javax.ws.rs:javax.ws.rs-api:$versionJavaxRs")
    implementation("javax.validation:validation-api:$versionJavaxValidation")

    /** Apache commons**/
    implementation("commons-io:commons-io:$versionApacheCommons")
    implementation("org.apache.commons:commons-lang3:3.12.0")

    /** Jackson **/
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.13.0")

    /** JsonB **/
    implementation("com.vladmihalcea:hibernate-types-55:2.14.1")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    /** JUnit **/
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-params")

    /** okhttp3 **/
//    implementation("com.squareup.okhttp3:okhttp:$versionSquareupOkhttp3Okhttp")
//    implementation("com.squareup.okhttp3:mockwebserver:$versionSquareupOkhttp2Mockwebserver")

    implementation("org.springframework.boot:spring-boot-starter-test")
    constraints {
        val karateVersion = "1.1.0"
        implementation("com.intuit.karate:karate-gatling:$karateVersion")
        implementation("com.intuit.karate:karate-junit5:$karateVersion")
    }
    implementation("org.apache.logging.log4j:log4j-api:2.16.0") {
        exclude(group = "org.apache.logging.log4j:2.*.*")
    }
    implementation("org.apache.logging.log4j:log4j-core:2.16.0") {
        exclude(group = "org.apache.logging.log4j:2.*.*")
    }
}

// Defines an additional test task that only runs tests with the "integration" tag.
task<Test>("integrationTest") {
    description = "Runs integration tests."

    useJUnitPlatform {
        includeTags("integration")
    }
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
            jvmTarget = "11"
            apiVersion = "1.6"
            languageVersion = "1.6"
        }
    }

    // Configure all tasks of type test
    withType<Test>().configureEach {
        systemProperty("spring.cloud.kubernetes.enabled", false)
        systemProperty("spring.cloud.loadbalancer.enabled", false)
//        systemProperty("spring.cloud.vault.enabled", false)
        useJUnitPlatform()
    }

    // Configure main test task (primarily for unit tests)
    tasks.test {
        useJUnitPlatform {
            excludeTags("integration")
        }
    }
}


tasks.test {
    finalizedBy(tasks.jacocoTestReport)

    extensions.configure(JacocoTaskExtension::class) {
        setDestinationFile(
            layout.buildDirectory.file("${project.buildDir}/coverage-reports/jacoco-ut.exec")
                .get().asFile
        )

    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
//    afterEvaluate {
//        classDirectories.from(files(classDirectories.files.map {
//            fileTree(it) {
//                exclude("**/*Config.java")
//                exclude("**/*Mapper.java")
//            }
//        }))
//    }
    reports {
        html.outputLocation.set(File("${project.projectDir}/target/coverage-reports"))
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
//        from(components["java"])
        pom {
            scm {
//                url.set("scm:git:https://github.ondemandenv.dev/engineering/cdk-spring-rds-platform.git")
//                connection.set("scm:git:https://github.ondemandenv.dev/engineering/cdk-spring-rds-platform.git")
//                developerConnection.set("scm:git:https://github.ondemandenv.dev/engineering/cdk-spring-rds-platform.git")
//                tag.set("HEAD")
            }
        }
    }
}

jacoco {

}
