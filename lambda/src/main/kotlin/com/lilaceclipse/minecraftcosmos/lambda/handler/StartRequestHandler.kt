package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.*
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest.StartRequest
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse.StartResponse
import com.lilaceclipse.minecraftcosmos.lambda.model.OnlineStatus
import com.lilaceclipse.minecraftcosmos.lambda.model.ServerEntry
import com.lilaceclipse.minecraftcosmos.lambda.storage.DynamoStorage
import com.lilaceclipse.minecraftcosmos.lambda.util.EnvVarProvider
import com.lilaceclipse.minecraftcosmos.lambda.util.SnsUtil
import mu.KotlinLogging
import javax.inject.Inject


class StartRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val dynamoStorage: DynamoStorage,
    private val ecsClient: AmazonECS,
    private val snsUtil: SnsUtil
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: StartRequest): CosmosResponse {
        if (listActiveTasks().taskArns.size != 0) {
            log.info { "Received request to start service, but it was already running" }
            return StartResponse(
                message = "Cosmos is already started!"
            )
        }

        log.info { "Received request to start service, will now attempt to start" }
        val runTaskRequest = RunTaskRequest()
            .withLaunchType(LaunchType.FARGATE)
            .withTaskDefinition(envVarProvider.taskDefinitionArn)
            .withCluster(envVarProvider.clusterArn)
            .withNetworkConfiguration(NetworkConfiguration()
                .withAwsvpcConfiguration(AwsVpcConfiguration()
                    .withAssignPublicIp(AssignPublicIp.ENABLED)
                    .withSecurityGroups(envVarProvider.securityGroupId)
                    .withSubnets(envVarProvider.subnetId)))
            .withOverrides(TaskOverride()
                .withContainerOverrides(ContainerOverride()
                    .withName("cosmos-container")
                    .withCommand(
                        "--environment", envVarProvider.stage,
                        "--target-server", request.serverUUID)))

        ecsClient.runTask(runTaskRequest)
        snsUtil.sendSmsAlert("Cosmos has started! Check cosmos.lilaceclipse.com for the server IP")
        dynamoStorage.updateServerEntryNonNulls(ServerEntry(
            serverId = request.serverUUID,
            onlineStatus = OnlineStatus.CONTAINER_LAUNCHED
        ))

        return StartResponse(
            message = "Cosmos will now start, refresh the page shortly to get the IP address!"
        )
    }

    private fun listActiveTasks(): ListTasksResult {
        return ecsClient.listTasks(
            ListTasksRequest()
            .withCluster(envVarProvider.clusterArn))
    }
}