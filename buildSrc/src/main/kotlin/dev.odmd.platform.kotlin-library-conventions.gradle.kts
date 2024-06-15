import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("dev.odmd.platform.kotlin-common-conventions")
}

tasks {
    getByName<BootBuildImage>("bootBuildImage") {
        enabled = false
    }

    getByName<BootJar>("bootJar") {
        enabled = false
    }

    getByName<Jar>("jar") {
        enabled = true
    }
}