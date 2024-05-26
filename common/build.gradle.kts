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
    kotlin("plugin.serialization")
}

dependencies {
    // AWS
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.25.31")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
