package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.*
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest.StartRequest
import com.lilaceclipse.minecraftcosmos.lambda.util.EnvVarProvider
import com.lilaceclipse.minecraftcosmos.lambda.util.ResponseUtil
import com.lilaceclipse.minecraftcosmos.lambda.util.SnsUtil
import javax.inject.Inject


class StartRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val responseUtil: ResponseUtil,
    private val ecsClient: AmazonECS,
    private val snsUtil: SnsUtil
) {

    fun handleRequest(request: StartRequest): APIGatewayProxyResponseEvent {
        try {
            if (listActiveTasks().taskArns.size != 0) {
                println("Received request to start service, but it was already running")
                return responseUtil.generateResponse(mapOf(
                    "message" to "Cosmos is already started!"
                ))
            }

            println("Received request to start service, will now attempt to start")
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
            return responseUtil.generateResponse(mapOf(
                "message" to "Cosmos will now start, refresh the page shortly to get the IP address!"
            ))
        } catch (e: Exception) {
            e.printStackTrace()
            return responseUtil.generateResponse(mapOf(
                "message" to "Something went wrong..."
            ))
        }
    }

    private fun listActiveTasks(): ListTasksResult {
        return ecsClient.listTasks(
            ListTasksRequest()
            .withCluster(envVarProvider.clusterArn))
    }
}