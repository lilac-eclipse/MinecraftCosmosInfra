package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.model.DescribeTasksRequest
import com.amazonaws.services.ecs.model.ListTasksRequest
import com.amazonaws.services.ecs.model.ListTasksResult
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest.StatusRequest
import com.lilaceclipse.minecraftcosmos.lambda.util.EnvVarProvider
import com.lilaceclipse.minecraftcosmos.lambda.util.ResponseUtil
import mu.KotlinLogging
import javax.inject.Inject

class StatusRequestHandler @Inject constructor(
    private val envVarProvider: EnvVarProvider,
    private val responseUtil: ResponseUtil,
    private val ecsClient: AmazonECS,
    private val ec2Client: AmazonEC2
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: StatusRequest): APIGatewayProxyResponseEvent {
        // This code assumes only one task
        var status: String // RUNNING, STARTING, STOPPED, ERROR
        var ip = ""
        try {
            log.info { "Fetching active tasks" }
            val listTaskResult = listActiveTasks()

            log.info { "Determining status" }
            when (listTaskResult.taskArns.size) {
                0 -> {
                    status = "STOPPED"
                }
                1 -> {
                    status = "RUNNING"
                    val describeTaskResult = ecsClient.describeTasks(
                        DescribeTasksRequest()
                        .withTasks(listTaskResult.taskArns[0])
                        .withCluster(envVarProvider.clusterArn))

                    val elasticNetworkInterface = describeTaskResult
                        .tasks[0]
                        .attachments[0]
                        .details.first { it.name == "networkInterfaceId" }
                        .value

                    val describeEniResult = ec2Client.describeNetworkInterfaces(
                        DescribeNetworkInterfacesRequest()
                        .withNetworkInterfaceIds(elasticNetworkInterface))

                    ip = describeEniResult.networkInterfaces[0].association.publicIp
                }
                else -> status = "ERROR"
            }
        } catch (e: Exception) {
            status = "ERROR"
            ip = ""
        }

        log.info { "Response Handled" }
        return responseUtil.generateResponse(mapOf(
            "status" to status,
            "ip" to ip
        ))
    }

    private fun listActiveTasks(): ListTasksResult {
        return ecsClient.listTasks(
            ListTasksRequest()
                .withCluster(envVarProvider.clusterArn))
    }
}