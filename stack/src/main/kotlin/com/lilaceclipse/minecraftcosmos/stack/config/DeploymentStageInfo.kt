package com.lilaceclipse.minecraftcosmos.stack.config

interface DeploymentStageInfo {
    val stageSuffix: String
    val isProd: Boolean
    val siteBucketName: String
    val serverDataBucketName: String
}

class ProdStage(
    override val stageSuffix: String = "prod",
    override val isProd: Boolean = true,
    override val siteBucketName: String = "cosmos.lilaceclipse.com",
    override val serverDataBucketName: String = "mccosmos-data-prod"
    ) : DeploymentStageInfo

class BetaStage(
    override val stageSuffix: String = "beta",
    override val isProd: Boolean = false,
    override val siteBucketName: String = "beta.cosmos.lilaceclipse.com",
    override val serverDataBucketName: String = "mccosmos-data-beta"
) : DeploymentStageInfo

val deploymentStages = listOf(ProdStage(), BetaStage())