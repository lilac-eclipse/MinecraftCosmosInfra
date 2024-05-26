package com.lilaceclipse.cosmos.cdk

import com.lilaceclipse.cosmos.cdk.config.deploymentStages
import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val environment = Environment.builder()
        .account("252475162445")
        .region("us-west-2")
        .build()

    for (stage in deploymentStages) {

        MinecraftCosmosStack(
            app,
            "${MinecraftCosmosStack.STACK_NAME}-${stage.stageSuffix}",
            StackProps.builder()
                .env(environment)
                .terminationProtection(true)
                .build(),
            MinecraftCosmosStackProps(stageInfo = stage)
        )
    }

    app.synth()
}