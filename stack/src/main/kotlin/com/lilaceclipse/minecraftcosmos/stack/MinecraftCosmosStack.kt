package com.lilaceclipse.minecraftcosmos.stack

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import software.amazon.awscdk.services.s3.BlockPublicAccess
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.ObjectOwnership
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.sns.Topic
import software.constructs.Construct
import java.io.File


data class MinecraftCosmosStackProps(
    val stageInfo: DeploymentStageInfo
): StackProps

class MinecraftCosmosStack(
    scope: Construct, id: String, props: StackProps, additionalStackProps: MinecraftCosmosStackProps
) : Stack(scope, id, props) {

    private val stageSuffix = additionalStackProps.stageInfo.stageSuffix
    init {
        val lambdaFunction = createLambdaFunction()
        val eventNotificationTopic = createEventNotificationTopic(lambdaFunction)
        val api = createApiGateway(lambdaFunction)
        val siteBucket = createSiteBucket(additionalStackProps, api)
        val serverDataBucket = createServerDataBucket(additionalStackProps)
        val (vpc, securityGroup) = createVpcAndSecurityGroup()
        val cluster = createEcsCluster(vpc)
        val (task, repository) = createTaskDefinition(serverDataBucket)
        configureLambdaEnvVars(lambdaFunction, cluster, task, securityGroup, vpc, eventNotificationTopic)
    }

    private fun createLambdaFunction(): Function {
        val lambdaFunction = Function.Builder.create(this, "mc-cosmos-lambda-$stageSuffix")
            .functionName("MinecraftCosmos-$stageSuffix")
            .code(Code.fromAsset("../lambda/build/libs/lambda-all.jar"))
            .handler("com.lilaceclipse.minecraftcosmos.lambda.MinecraftCosmosLambdaHandler")
            .timeout(Duration.seconds(5))
            .memorySize(1024)
            .timeout(Duration.seconds(30))
            .runtime(Runtime.JAVA_11)
            .build()

        // TODO remove full access
        lambdaFunction.role!!.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2FullAccess"))
        lambdaFunction.role!!.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonECS_FullAccess"))

        return lambdaFunction
    }

    private fun createEventNotificationTopic(lambdaFunction: Function): Topic {
        val eventNotificationTopic = Topic(this, "statusAlertTopic-$stageSuffix")
        eventNotificationTopic.grantPublish(lambdaFunction)

        return eventNotificationTopic
    }

    private fun createApiGateway(lambdaFunction: Function): RestApi {
        val api = RestApi.Builder.create(this, "mc-cosmos-api-$stageSuffix")
            .restApiName("Cosmos API - $stageSuffix")
            .description("Handle API requests for MC Cosmos")
            .build()

        val templates = mapOf(
            "application/json" to "{ \"statusCode\": \"200\" }"
        )
        val lambdaIntegration = LambdaIntegration.Builder.create(lambdaFunction)
            .requestTemplates(templates)
            .build()

        api.root.addMethod("POST", lambdaIntegration)

        return api
    }

    private fun createSiteBucket(additionalStackProps: MinecraftCosmosStackProps, api: RestApi): Bucket {
        val siteBucket = Bucket.Builder.create(this, "mc-cosmos-static-site-$stageSuffix")
            .bucketName(additionalStackProps.stageInfo.siteBucketName)
            .publicReadAccess(true)
            // See this issue for why this enables public access: https://github.com/aws/aws-cdk/issues/25983
            .blockPublicAccess(BlockPublicAccess.Builder.create()
                .blockPublicAcls(false)
                .blockPublicPolicy(false)
                .ignorePublicAcls(false)
                .restrictPublicBuckets(false)
                .build())
            .objectOwnership(ObjectOwnership.OBJECT_WRITER)
            .removalPolicy(RemovalPolicy.DESTROY)
            .websiteIndexDocument("index.html")
            .build()

        // Load site config file template and populate with data from CDK
        val configFile = File("site-config/config-template-${additionalStackProps.stageInfo.stageSuffix}.json")
        val config = jacksonObjectMapper().readValue(configFile, Map::class.java).toMutableMap()
        config["cosmosApiEndpoint"] = api.url

        BucketDeployment.Builder.create(this, "deploy-static-site-$stageSuffix")
            .sources(listOf(
                Source.asset("../static-site"),
                Source.jsonData("config.json", config)))
            .destinationBucket(siteBucket)
            .prune(false)
            .build()

        return siteBucket
    }

    private fun createServerDataBucket(additionalStackProps: MinecraftCosmosStackProps): Bucket {

        val serverDataBucket = Bucket.Builder.create(this, "mc-cosmos-data-$stageSuffix")
            .bucketName(additionalStackProps.stageInfo.serverDataBucketName)
            .versioned(true)
            .removalPolicy(RemovalPolicy.RETAIN)
            .build()

        return serverDataBucket
    }

    private fun createVpcAndSecurityGroup(): Pair<Vpc, SecurityGroup> {
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

        return Pair(vpc, securityGroup)
    }

    private fun createEcsCluster(vpc: Vpc): Cluster {
        return Cluster(this, "mc-cosmos-cluster-$stageSuffix", ClusterProps.builder()
            .vpc(vpc)
            .build())
    }

    private fun createTaskDefinition(serverDataBucket: Bucket): Pair<FargateTaskDefinition, Repository> {
        val task = FargateTaskDefinition(this, "mc-cosmos-task-$stageSuffix",
            FargateTaskDefinitionProps.builder()
                .memoryLimitMiB(8192)
                .cpu(2048)
                .build()
        )
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
            .healthCheck(HealthCheck.builder()
                .command(listOf("CMD-SHELL", "curl -f http://localhost/health || exit 1"))
                .interval(Duration.minutes(1))
                .startPeriod(Duration.seconds(30))
                .timeout(Duration.seconds(5))
                .build())
            .build())

        return Pair(task, repository)
    }

    private fun configureLambdaEnvVars(
        lambdaFunction: Function,
        cluster: Cluster,
        task: FargateTaskDefinition,
        securityGroup: SecurityGroup,
        vpc: Vpc,
        eventNotificationTopic: Topic
    ) {
        lambdaFunction.addEnvironment("CLUSTER_ARN", cluster.clusterArn)
        lambdaFunction.addEnvironment("TASK_DEFINITION_ARN", task.taskDefinitionArn)
        lambdaFunction.addEnvironment("SECURITY_GROUP_ID", securityGroup.securityGroupId)
        lambdaFunction.addEnvironment("SUBNET_ID", vpc.publicSubnets[0].subnetId)
        lambdaFunction.addEnvironment("STATUS_ALERT_TOPIC_ARN", eventNotificationTopic.topicArn)
    }

    companion object {
        const val STACK_NAME = "MinecraftCosmosStack"
    }
}