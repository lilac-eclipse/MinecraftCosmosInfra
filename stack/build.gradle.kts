/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    id("MinecraftCosmosInfra.kotlin-stack-conventions")
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.42.0")
}

application {
    mainClass.set("com.lilaceclipse.minecraftcosmos.stack.AppKt")
}
