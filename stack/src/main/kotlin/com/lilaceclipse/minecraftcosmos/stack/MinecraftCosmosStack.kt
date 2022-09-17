package com.lilaceclipse.minecraftcosmos.stack

import software.amazon.awscdk.Duration
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.apigateway.LambdaIntegration
import software.amazon.awscdk.services.apigateway.RestApi
import software.amazon.awscdk.services.events.targets.ApiGateway
import software.amazon.awscdk.services.iam.PolicyStatement
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.ses.actions.Sns
import software.amazon.awscdk.services.sns.Topic
import software.amazon.awscdk.services.sns.subscriptions.SmsSubscription
import software.constructs.Construct


class MinecraftCosmosStack(
    scope: Construct, id: String, props: StackProps
) : Stack(scope, id, props) {
    init {

        val statusAlertTopic = Topic(this, "statusAlertTopic")

        val lambdaFunction = Function.Builder.create(this, "MinecraftCosmosLambda")
            .functionName("MinecraftCosmos")
            .code(Code.fromAsset("../lambda/build/libs/lambda-all.jar"))
            .handler("com.lilaceclipse.minecraftcosmos.lambda.MinecraftCosmosLambdaHandler")
            .timeout(Duration.seconds(5))
            .memorySize(1024)
            .timeout(Duration.seconds(30))
            .runtime(Runtime.JAVA_11)
            .environment(mapOf(
                "STATUS_ALERT_TOPIC_ARN" to statusAlertTopic.topicArn
            ))
            .build()
        statusAlertTopic.grantPublish(lambdaFunction)

        val api = RestApi.Builder.create(this, "cosmos-api")
            .restApiName("Cosmos API")
            .description("Handle api requests for MC cosmos")
            .build()
        val templates = mapOf(
            "application/json" to "{ \"statusCode\": \"200\" }"
        )
        val dealIntegration = LambdaIntegration.Builder.create(lambdaFunction)
            .requestTemplates(templates)
            .build()
        api.root
            .addResource("start")
            .addMethod("POST", dealIntegration)

        val siteBucket = Bucket.Builder.create(this, "mccosmos-static-site")
            .bucketName("mccosmos-static-site")
            .publicReadAccess(true)
            .removalPolicy(RemovalPolicy.DESTROY)
            .websiteIndexDocument("index.html")
            .build()

        BucketDeployment.Builder.create(this, "deploy-static-site")
            .sources(listOf(Source.asset("../static-site")))
            .destinationBucket(siteBucket)
            .build()
    }
}