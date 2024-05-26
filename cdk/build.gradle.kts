/*
 * This file configures the build for the 'stack' subproject.
 * It applies the common-conventions plugin to inherit shared build configuration,
 * and the application plugin to build a CLI application.
 * Additional dependencies specific to the 'stack' subproject are included,
 * such as the AWS CDK library and the Jackson Kotlin module for JSON serialization.
 */

plugins {
    // Apply the common convention plugin for shared build configuration between library and application projects.
    id("MinecraftCosmosInfra.kotlin-common-conventions")

    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.134.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
}

application {
    mainClass.set("com.lilaceclipse.cosmos.cdk.AppKt")
}

