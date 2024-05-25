/*
 * This file defines a custom plugin that encapsulates common build configuration and conventions
 * for Kotlin projects in the MinecraftCosmos infrastructure.
 * It applies the Kotlin JVM plugin, sets up the maven-central repository,
 * and includes common dependencies such as kotlin-stdlib-jdk8 and JUnit for testing.
 * Subprojects can apply this plugin to inherit the shared build configuration.
 */

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    kotlin("jvm")
    kotlin("kapt")
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    constraints {
        // Define dependency versions as constraints
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    // Logging libraries
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.4")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.20.0")

    // Dagger for dependency injection
    implementation("com.google.dagger:dagger:2.51.1")
    kapt("com.google.dagger:dagger-compiler:2.51.1")
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

kapt {
    arguments {
        arg("project", "${project.group}${project.name}")
    }
}