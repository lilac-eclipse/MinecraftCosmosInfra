package com.lilaceclipse.minecraftcosmos.stack

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lilaceclipse.minecraftcosmos.stack.config.DeploymentStageInfo
import software.amazon.awscdk.Duration
import software.amazon.awscdk.RemovalPolicy
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.apigateway.LambdaIntegration
import software.amazon.awscdk.services.apigateway.RestApi
import software.amazon.awscdk.services.dynamodb.*
import software.amazon.awscdk.services.ec2.*
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecr.RepositoryProps
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.iam.ManagedPolicy
import software.amazon.awscdk.services.lambda.Alias
import software.amazon.awscdk.services.lambda.AliasProps
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime
import software.amazon.awscdk.services.lambda.SnapStartConf
import software.amazon.awscdk.services.logs.LogGroup
import software.amazon.awscdk.services.logs.LogGroupProps
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.s3.BlockPublicAccess
import software.amazon.awscdk.services.s3.Bucket
import software.amazon.awscdk.services.s3.ObjectOwnership
import software.amazon.awscdk.services.s3.deployment.BucketDeployment
import software.amazon.awscdk.services.s3.deployment.Source
import software.amazon.awscdk.services.sns.Topic
import software.constructs.Construct
import java.io.File
import software.amazon.awscdk.services.ecr.LifecycleRule as EcrLifecycleRule
import software.amazon.awscdk.services.s3.LifecycleRule as S3LifecycleRule


data class MinecraftCosmosStackProps(
    val stageInfo: DeploymentStageInfo
): StackProps

class MinecraftCosmosStack(
    scope: Construct, id: String, props: StackProps, additionalStackProps: MinecraftCosmosStackProps
) : Stack(scope, id, props) {

    private val stageInfo = additionalStackProps.stageInfo
    private val stageSuffix = additionalStackProps.stageInfo.stageSuffix
    init {
        val (lambdaFunction, lambdaVersion) = createLambdaFunction()
        val eventNotificationTopic = createEventNotificationTopic(lambdaFunction)
        val serverTable = createDynamoDbTable()
        val api = createApiGateway(lambdaVersion)
        val siteBucket = createSiteBucket(api)
        val serverDataBucket = createServerDataBucket()
        val (vpc, securityGroup) = createVpcAndSecurityGroup()
        val cluster = createEcsCluster(vpc)
        val (task, repository) = createTaskDefinition(serverDataBucket, serverTable)
        configureLambdaEnvVars(lambdaFunction, cluster, task, securityGroup, vpc, eventNotificationTopic, serverTable)
    }

    private fun createLambdaFunction(): Pair<Function, Alias> {
        val lambdaFunction = Function.Builder.create(this, "mc-cosmos-lambda-$stageSuffix")
            .functionName("MinecraftCosmos-$stageSuffix")
            .code(Code.fromAsset("../lambda/build/libs/lambda-all.jar"))
            .handler("com.lilaceclipse.minecraftcosmos.lambda.MinecraftCosmosLambdaHandler")
            .runtime(Runtime.JAVA_11)
            .memorySize(1024)
            .timeout(Duration.seconds(30))
            .snapStart(SnapStartConf.ON_PUBLISHED_VERSIONS)
            .build()

        val lambdaAlias = Alias(this, "mc-cosmos-lambda-alias-$stageSuffix", AliasProps.builder()
            .aliasName("mc-cosmos-lambda-alias-$stageSuffix")
            // Pointing this to currentVersion will cause a version to be created, which will in turn enable snapstart
            // This process takes several minutes, so we only enable it in prod
            .version(if (stageInfo.isProd) lambdaFunction.currentVersion else lambdaFunction.latestVersion)
            .build())

        // TODO remove full access
        lambdaFunction.role!!.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2FullAccess"))
        lambdaFunction.role!!.addManagedPolicy(ManagedPolicy.fromAwsManagedPolicyName("AmazonECS_FullAccess"))

        return Pair(lambdaFunction, lambdaAlias)
    }

    private fun createEventNotificationTopic(lambdaFunction: Function): Topic {
        val eventNotificationTopic = Topic(this, "statusAlertTopic-$stageSuffix")
        eventNotificationTopic.grantPublish(lambdaFunction)

        return eventNotificationTopic
    }

    private fun createDynamoDbTable(): TableV2 {
        val serverTable = TableV2.Builder.create(this, "cosmos-server-table-$stageSuffix")
            .tableName("CosmosServers-$stageSuffix")
            .partitionKey(Attribute.builder()
                .name("serverId")
                .type(AttributeType.STRING)
                .build())
            .deletionProtection(true)
            .build()

        return serverTable
    }

    private fun createApiGateway(lambdaAlias: Alias): RestApi {
        val api = RestApi.Builder.create(this, "mc-cosmos-api-$stageSuffix")
            .restApiName("Cosmos API - $stageSuffix")
            .description("Handle API requests for MC Cosmos")
            .build()

        val templates = mapOf(
            "application/json" to "{ \"statusCode\": \"200\" }"
        )
        val lambdaIntegration = LambdaIntegration.Builder.create(lambdaAlias)
            .requestTemplates(templates)
            .build()

        api.root.addMethod("POST", lambdaIntegration)

        return api
    }

    private fun createSiteBucket(api: RestApi): Bucket {
        val siteBucket = Bucket.Builder.create(this, "mc-cosmos-static-site-$stageSuffix")
            .bucketName(stageInfo.siteBucketName)
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
        val configFile = File("site-config/config-template-$stageSuffix.json")
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

    private fun createServerDataBucket(): Bucket {

        val serverDataBucket = Bucket.Builder.create(this, "mc-cosmos-data-$stageSuffix")
            .bucketName(stageInfo.serverDataBucketName)
            .versioned(true)
            .removalPolicy(RemovalPolicy.RETAIN)
            .lifecycleRules(listOf(
                S3LifecycleRule.Builder()
                    .noncurrentVersionExpiration(Duration.days(30))
                    .noncurrentVersionsToRetain(5)
                    .build()
            ))
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

    private fun createTaskDefinition(serverDataBucket: Bucket, serverTable: TableV2): Pair<FargateTaskDefinition, Repository> {
        val task = FargateTaskDefinition(this, "mc-cosmos-task-$stageSuffix",
            FargateTaskDefinitionProps.builder()
                .memoryLimitMiB(8192)
                .cpu(2048)
                .build()
        )
        serverDataBucket.grantReadWrite(task.taskRole)
        serverTable.grantFullAccess(task.taskRole)

        val repository = Repository(this, "mc-cosmos-repo-$stageSuffix", RepositoryProps.builder()
            .repositoryName("mc-cosmos-repo-$stageSuffix")
            .lifecycleRules(listOf(
                EcrLifecycleRule.builder()
                    .rulePriority(1)
                    .description("Keep only the latest image")
                    .maxImageCount(1)
                    .build()
            ))
            .build())

        val logGroup = LogGroup(this, "mc-cosmos-ecs-logs-$stageSuffix", LogGroupProps.builder()
            .logGroupName("mc-cosmos-ecs-logs-$stageSuffix")
            .retention(RetentionDays.ONE_WEEK)
            .build())

        task.addContainer("mc-cosmos-task-container-$stageSuffix", ContainerDefinitionOptions.builder()
            .containerName("cosmos-container")
            .image(ContainerImage.fromEcrRepository(repository))
            .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                .logGroup(logGroup)
                .streamPrefix("mc-cosmos-ecs-container-logs")
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
        eventNotificationTopic: Topic,
        serverTable: TableV2
    ) {

        val envVars = mapOf(
            "CLUSTER_ARN" to cluster.clusterArn,
            "TASK_DEFINITION_ARN" to task.taskDefinitionArn,
            "SECURITY_GROUP_ID" to securityGroup.securityGroupId,
            "SUBNET_ID" to vpc.publicSubnets[0].subnetId,
            "STATUS_ALERT_TOPIC_ARN" to eventNotificationTopic.topicArn,
            "SERVER_TABLE_NAME" to serverTable.tableName,
            "STAGE" to stageInfo.stageSuffix
        )

        envVars.forEach { (key, value) ->
            lambdaFunction.addEnvironment(key, value)
        }
    }

    companion object {
        const val STACK_NAME = "MinecraftCosmosStack"
    }
}