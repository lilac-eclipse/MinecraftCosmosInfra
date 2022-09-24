package com.lilaceclipse.minecraftcosmos.stack.config

interface DeploymentStageInfo {
    val stageSuffix: String
    val siteBucketName: String
    val serverDataBucketName: String
}

class ProdStage(
    override val stageSuffix: String = "prod",
    override val siteBucketName: String = "cosmos.cryo3.net",
    override val serverDataBucketName: String = "mccosmos-server-data"
    ) : DeploymentStageInfo

class BetaStage(
    override val stageSuffix: String = "beta",
    override val siteBucketName: String = "beta.cosmos.cryo3.net",
    override val serverDataBucketName: String = "mccosmos-server-data-beta"
) : DeploymentStageInfo

val deploymentStages = listOf(ProdStage(), BetaStage())