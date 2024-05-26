import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

/*
 * This file configures the build for the 'lambda' subproject.
 * It applies the common-conventions plugin to inherit shared build configuration,
 * the shadow plugin for creating a fat JAR, and the Kotlin serialization plugin.
 * The subproject includes dependencies specific to AWS Lambda development,
 * such as the AWS Lambda Java core and events libraries, and the AWS SDK for SNS, EC2, and ECS.
 * It also includes the kotlinx-serialization-json library for JSON serialization.
 */

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("MinecraftCosmosInfra.kotlin-common-conventions")

    application
    id("com.github.johnrengelman.shadow") version "7.0.0"

}

dependencies {
    // Common libs
    implementation(project(":common"))

    // UI
    implementation("com.formdev:flatlaf:3.4.1")
}

application {
    mainClass.set("com.lilaceclipse.cosmos.client.MainKt")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("Cosmos-Client.jar")
}