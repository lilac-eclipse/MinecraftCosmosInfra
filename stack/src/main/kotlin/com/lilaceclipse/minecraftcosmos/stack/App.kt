package com.lilaceclipse.minecraftcosmos.stack

import com.lilaceclipse.minecraftcosmos.stack.config.deploymentStages
import software.amazon.awscdk.App
import software.amazon.awscdk.Environment
import software.amazon.awscdk.StackProps

fun main() {
    val app = App()

    val environment = Environment.builder()
        .account("252475162445")
        .region("us-west-1")
        .build()

    for (stage in deploymentStages) {

        MinecraftCosmosStack(
            app,
            "${MinecraftCosmosStack.STACK_NAME}-${stage.stageSuffix}",
            StackProps.builder()
                .env(environment)
                .build(),
            MinecraftCosmosStackProps(stageInfo = stage)
        )
    }

    app.synth()
}