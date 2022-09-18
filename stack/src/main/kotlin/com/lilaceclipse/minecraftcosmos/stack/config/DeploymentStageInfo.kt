package com.lilaceclipse.minecraftcosmos.stack.config

interface DeploymentStageInfo {
    val stageSuffix: String
    val siteBucketName: String
}

class ProdStage(
    override val stageSuffix: String = "prod",
    override val siteBucketName: String = "mccosmos-static-site"
    ) : DeploymentStageInfo

class BetaStage(
    override val stageSuffix: String = "beta",
    override val siteBucketName: String = "mccosmos-static-site-beta"
) : DeploymentStageInfo

val deploymentStages = listOf(ProdStage(), BetaStage())