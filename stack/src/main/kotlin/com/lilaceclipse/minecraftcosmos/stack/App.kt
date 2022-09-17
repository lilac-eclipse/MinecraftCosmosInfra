package com.lilaceclipse.minecraftcosmos.stack

import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

//import software.amazon.awscdk.core.App

//import software.amazon.awscdk.core.App
//import software.amazon.awscdk.core.Environment
//import software.amazon.awscdk.core.StackProps

fun main() {
    val app = App()

    MinecraftCosmosStack(
        app, "MinecraftCosmosStack", StackProps.builder()
            .env(
                Environment.builder()
                    .account("252475162445")
                    .region("us-west-1")
                    .build()
            )
            .build()
    )
    app.synth()
}