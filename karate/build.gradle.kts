plugins {
    id("dev.odmd.platform.kotlin-library-conventions")
    id("scala")
}

dependencies {
    val karateVersion = "1.2.0"
    testImplementation("com.intuit.karate:karate-gatling:$karateVersion")
    testImplementation("com.intuit.karate:karate-junit5:$karateVersion")
    testImplementation("net.masterthought:cucumber-reporting:5.6.1")
}
sourceSets {
    test {
        resources {
            // add `src/test/kotlin` as a resource directory so feature files can live next to code

            srcDirs("src/test/kotlin")
            exclude("**/*.scala")
        }
    }
}
tasks.register<Test>("e2e") {
    useJUnitPlatform()
    systemProperty("karate.env", System.getProperty("karate.env"))
    systemProperty(
        "karate.options",
        System.getProperty("karate.options", "").plus(" --tags @e2e")
    )
}
tasks.register<JavaExec>("gatling") {
    group = "Perf Tests"
    description = "Run Gatling Tests"
    classpath(sourceSets["test"].runtimeClasspath)
    file("${buildDir}/reports/gatling").mkdirs()
    mainClass.set("io.gatling.app.Gatling")
    args(System.getProperty("gatling.simulationClass")?.let {
        mutableListOf(
            "-s", it,
            "-rf", "${buildDir}/reports/gatling"
        )
    } ?: emptyList<String>())
    systemProperty(
        "karate.options",
        System.getProperty("karate.options", "").plus(" --tags @perf")
    )
    systemProperty("karate.env", System.getProperty("karate.env"))
}
tasks.test {
    exclude("**")
}

description = "karate"
