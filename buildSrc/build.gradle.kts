/*
 * This file configures the build logic for the buildSrc directory.
 * It sets up the necessary plugins and dependencies for custom build logic and plugins.
 * The kotlin-dsl plugin is applied to support writing Gradle build scripts in Kotlin.
 * The kotlin-gradle-plugin dependency is included to provide support for building Kotlin projects.
 */

plugins {
    // Support convention plugins written in Kotlin. Convention plugins are build scripts in 'src/main' that automatically become available as plugins in the main build.
    `kotlin-dsl`
    kotlin("jvm") version "1.7.10"
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-serialization")
}
