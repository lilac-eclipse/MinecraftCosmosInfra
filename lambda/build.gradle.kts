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
    kotlin("jvm")
    kotlin("kapt")

    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // AWS client dependencies
    implementation("com.amazonaws:aws-java-sdk-sns:1.12.691")
    implementation("com.amazonaws:aws-java-sdk-ec2:1.12.691")
    implementation("com.amazonaws:aws-java-sdk-ecs:1.12.691")
    implementation("software.amazon.awssdk:dynamodb-enhanced:2.25.31")

    // Dagger for dependency injection
    implementation("com.google.dagger:dagger:2.51.1")
    kapt("com.google.dagger:dagger-compiler:2.51.1")

    // Logging libraries
    implementation("io.github.microutils:kotlin-logging:2.1.23")
    implementation("org.slf4j:slf4j-simple:1.7.30")
}

kapt {
    arguments {
        arg("project", "${project.group}${project.name}")
    }
}