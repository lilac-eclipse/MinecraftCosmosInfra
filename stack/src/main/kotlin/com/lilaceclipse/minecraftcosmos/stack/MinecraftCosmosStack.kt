package com.lilaceclipse.minecraftcosmos.stack

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.constructs.Construct


class MinecraftCosmosStack(
    scope: Construct, id: String, props: StackProps
) : Stack(scope, id, props) {
    init {
        Function.Builder.create(this, "MinecraftCosmosLambda")
            .functionName("MinecraftCosmos")
            .code(Code.fromAsset("../lambda/build/libs/lambda-all.jar"))
            .handler("com.lilaceclipse.minecraftcosmos.lambda.MinecraftCosmosLambdaHandler")
            .timeout(Duration.seconds(5))
            .memorySize(1024)
            .runtime(Runtime.JAVA_11)
            .build()
    }
}