package com.lilaceclipse.cosmos.cdk.config

interface DeploymentStageInfo {
    val stageSuffix: String
    val isProd: Boolean
    val siteBucketName: String
    val serverDataBucketName: String
    val domainName: String
    val hostedZoneId: String
}

class ProdStage(
    override val stageSuffix: String = "prod",
    override val isProd: Boolean = true,
    override val siteBucketName: String = "cosmos.lilaceclipse.com",
    override val serverDataBucketName: String = "mccosmos-data-prod",
    override val domainName: String = "cosmos.lilaceclipse.com",
    override val hostedZoneId: String = "Z04300172AOIQ9OTZZ7SQ"
    ) : DeploymentStageInfo

class BetaStage(
    override val stageSuffix: String = "beta",
    override val isProd: Boolean = false,
    override val siteBucketName: String = "beta.cosmos.lilaceclipse.com",
    override val serverDataBucketName: String = "mccosmos-data-beta",
    override val domainName: String = "beta.cosmos.lilaceclipse.com",
    override val hostedZoneId: String = "Z04300172AOIQ9OTZZ7SQ"
) : DeploymentStageInfo

val deploymentStages = listOf(ProdStage(), BetaStage())