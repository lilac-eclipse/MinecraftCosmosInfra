package com.lilaceclipse.minecraftcosmos.stack

import com.lilaceclipse.minecraftcosmos.stack.config.DeploymentStageInfo
import software.amazon.awscdk.Duration
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.apigateway.LambdaIntegration
import software.amazon.awscdk.services.apigateway.RestApi
import software.amazon.awscdk.services.ec2.*
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecr.RepositoryProps
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.iam.ManagedPolicy
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.sns.Topic
import software.constructs.Construct


data class MinecraftCosmosStackProps(
    val stageInfo: DeploymentStageInfo
): StackProps

class MinecraftCosmosStack(
    scope: Construct, id: String, props: StackProps, additionalStackProps: MinecraftCosmosStackProps
) : Stack(scope, id, props) {

    init {

        val stageSuffix = additionalStackProps.stageInfo.stageSuffix

        val statusAlertTopic = Topic(this, "statusAlertTopic-$stageSuffix")

        val lambdaFunction = Function.Builder.create(this, "MinecraftCosmosLambda-$stageSuffix")
            .functionName("MinecraftCosmos-$stageSuffix")
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

        // TODO remove EC2 full access
        lambdaFunction.role!!.addManagedPolicy(
            ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2FullAccess"))

        val api = RestApi.Builder.create(this, "cosmos-api-$stageSuffix")
            .restApiName("Cosmos API - $stageSuffix")
            .description("Handle api requests for MC cosmos")
            .build()
        val templates = mapOf(
            "application/json" to "{ \"statusCode\": \"200\" }"
        )
        // TODO create separate prod/beta stage that point to separate lambdas
        val dealIntegration = LambdaIntegration.Builder.create(lambdaFunction)
            .requestTemplates(templates)
            .build()
        api.root
//            .addResource("start")
            .addMethod("POST", dealIntegration)

        val siteBucket = Bucket.Builder.create(this, "mccosmos-static-site-$stageSuffix")
            .bucketName(additionalStackProps.stageInfo.siteBucketName)
            .publicReadAccess(true)
            .removalPolicy(RemovalPolicy.DESTROY)
            .websiteIndexDocument("index.html")
            .build()

        val serverDataBucket = Bucket.Builder.create(this, "mccosmos-server-data-$stageSuffix")
            .bucketName(additionalStackProps.stageInfo.serverDataBucketName)
            .removalPolicy(RemovalPolicy.RETAIN)
            .build()

        BucketDeployment.Builder.create(this, "deploy-static-site-$stageSuffix")
            .sources(listOf(Source.asset("../static-site")))
            .destinationBucket(siteBucket)
            .prune(false)
            .build()


        val vpc = Vpc(this, "mc-cosmos-vpc-$stageSuffix", VpcProps.builder()
            .maxAzs(1)
            .natGateways(0)
            .subnetConfiguration(listOf(SubnetConfiguration.builder()
                .name("mc-cosmos-public-subnet-$stageSuffix")
                .subnetType(SubnetType.PUBLIC)
                .mapPublicIpOnLaunch(true)
                .build()))
            .cidr("10.0.0.0/16")
            .build())
        val securityGroup = SecurityGroup(this, "mc-cosmos-sg-$stageSuffix", SecurityGroupProps.builder()
            .vpc(vpc)
            .build())
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(25565))
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(80)) // TODO disable this (should only be accessible by lambda)

        Cluster(this, "mc-cosmos-cluster-$stageSuffix", ClusterProps.builder()
            .vpc(vpc)
            .build())

        val task = FargateTaskDefinition(this, "mc-cosmos-task-$stageSuffix", FargateTaskDefinitionProps.builder()
            .memoryLimitMiB(5120)
            .cpu(2048)
            .build())
        serverDataBucket.grantReadWrite(task.taskRole)

        val repository = Repository(this, "mc-cosmos-repo-$stageSuffix", RepositoryProps.builder()
            .repositoryName("mc-cosmos-repo-$stageSuffix")
            .build())

        // TODO set up lifecycle/auto delete
        task.addContainer("mc-cosmos-task-container-$stageSuffix", ContainerDefinitionOptions.builder()
            .image(ContainerImage.fromEcrRepository(repository))
            .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                .streamPrefix("mc-cosmos-ecs-logs")
                .logRetention(RetentionDays.ONE_WEEK)
                .build()))
            .portMappings(listOf(
                PortMapping.builder()
                    .containerPort(25565)
                    .hostPort(25565)
                    .build(),
                PortMapping.builder()
                    .containerPort(80)
                    .hostPort(80)
                    .build()))
            .build())
    }

    companion object {
        const val STACK_NAME = "MinecraftCosmosStack"
    }
}