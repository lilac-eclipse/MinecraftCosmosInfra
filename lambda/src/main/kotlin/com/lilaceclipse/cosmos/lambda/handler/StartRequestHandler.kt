package com.lilaceclipse.cosmos.lambda.handler

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.*
import com.lilaceclipse.cosmos.lambda.model.CosmosRequest.StartRequest
import com.lilaceclipse.cosmos.lambda.model.CosmosResponse
import com.lilaceclipse.cosmos.lambda.model.CosmosResponse.StartResponse
import com.lilaceclipse.cosmos.lambda.model.OnlineStatus
import com.lilaceclipse.cosmos.lambda.model.ServerEntry
import com.lilaceclipse.cosmos.lambda.storage.DynamoStorage
import com.lilaceclipse.cosmos.lambda.util.EnvVarProvider
import com.lilaceclipse.cosmos.lambda.util.SnsUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject


class StartRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val dynamoStorage: DynamoStorage,
    private val ecsClient: AmazonECS,
    private val snsUtil: SnsUtil
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: StartRequest): CosmosResponse {
        val serverEntry = dynamoStorage.getServerEntryFromDb(request.serverUUID)

        if (serverEntry.onlineStatus != OnlineStatus.OFFLINE) {
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
        dynamoStorage.updateServerEntryNonNulls(
            ServerEntry(
            serverId = request.serverUUID,
            onlineStatus = OnlineStatus.CONTAINER_LAUNCHED
        )
        )

        return StartResponse(
            message = "Cosmos will now start, refresh the page shortly to get the IP address!"
        )
    }
}