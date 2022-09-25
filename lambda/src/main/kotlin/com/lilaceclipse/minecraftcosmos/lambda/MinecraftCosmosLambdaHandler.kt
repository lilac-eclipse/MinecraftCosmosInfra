package com.lilaceclipse.minecraftcosmos.lambda

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.DescribeNetworkInterfacesRequest
import com.amazonaws.services.ec2.model.StartInstancesRequest
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.ecs.model.*
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.PublishRequest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * See: https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html
 */
class MinecraftCosmosLambdaHandler: RequestHandler<Map<String, Any>, APIGatewayProxyResponseEvent> {

    private val snsClient: AmazonSNS = AmazonSNSClientBuilder.defaultClient()
    private val ec2Client: AmazonEC2 = AmazonEC2ClientBuilder.defaultClient()
    private val ecsClient: AmazonECS = AmazonECSClientBuilder.defaultClient()

    private val smsAlertTopicArn = System.getenv("STATUS_ALERT_TOPIC_ARN")
    private val clusterArn = System.getenv("CLUSTER_ARN")
    private val taskDefinitionArn = System.getenv("TASK_DEFINITION_ARN")
    private val securityGroupId = System.getenv("SECURITY_GROUP_ID")
    private val subnetId = System.getenv("SUBNET_ID")

    override fun handleRequest(input: Map<String, Any>, context: Context): APIGatewayProxyResponseEvent {
        var requestBody: Map<String, String>? = null
        try {
            requestBody = Json.decodeFromString(input["body"] as String)
        } catch (e: Exception) {
            println("Failed to decode input body from: ${input["body"]}")
        }

        return if (requestBody?.get("requestType") == "STATUS") {
            handleStatusRequest()
        } else if (requestBody?.get("requestType") == "START") {
            handleStartRequest()
        } else {
            println(input)
            generateResponse(mapOf(
                "message" to "Request type not supported"
            ))
        }
    }

    private fun handleStatusRequest(): APIGatewayProxyResponseEvent {
        // This code assumes only one task
        var status: String // RUNNING, STARTING, STOPPED, ERROR
        var ip = ""
        try {
            val listTaskResult = listActiveTasks()

            when (listTaskResult.taskArns.size) {
                0 -> {
                    status = "STOPPED"
                }
                1 -> {
                    status = "RUNNING"
                    val describeTaskResult = ecsClient.describeTasks(DescribeTasksRequest()
                        .withTasks(listTaskResult.taskArns[0])
                        .withCluster(clusterArn))

                    val elasticNetworkInterface = describeTaskResult
                        .tasks[0]
                        .attachments[0]
                        .details.first { it.name == "networkInterfaceId" }
                        .value

                    val describeEniResult = ec2Client.describeNetworkInterfaces(DescribeNetworkInterfacesRequest()
                        .withNetworkInterfaceIds(elasticNetworkInterface))

                    ip = describeEniResult.networkInterfaces[0].association.publicIp
                }
                else -> status = "ERROR"
            }
        } catch (e: Exception) {
            status = "ERROR"
            ip = ""
        }

        return generateResponse(mapOf(
            "status" to status,
            "ip" to ip
        ))
    }

    private fun handleStartRequest(): APIGatewayProxyResponseEvent {

        try {
            if (listActiveTasks().taskArns.size != 0) {
                println("Received request to start service, but it was already running")
                return generateResponse(mapOf(
                    "message" to "Cosmos is already started!"
                ))
            }

            println("Received request to start service, will now attempt to start")
            val runTaskRequest = RunTaskRequest()
                .withLaunchType(LaunchType.FARGATE)
                .withTaskDefinition(taskDefinitionArn)
                .withCluster(clusterArn)
                .withNetworkConfiguration(NetworkConfiguration()
                    .withAwsvpcConfiguration(AwsVpcConfiguration()
                        .withAssignPublicIp(AssignPublicIp.ENABLED)
                        .withSecurityGroups(securityGroupId)
                        .withSubnets(subnetId)))
            ecsClient.runTask(runTaskRequest)

            sendSmsAlert("Cosmos has started! Check cosmos.cryo3.net for the server IP")
            return generateResponse(mapOf(
                "message" to "Cosmos will now start, refresh the page shortly to get the IP address! " +
                        "Automated messaging is currently unavailable, please alert people accordingly :)"
            ))
        } catch (e: Exception) {
            return generateResponse(mapOf(
                "message" to "Something went wrong..."
            ))
        }
    }

    private fun listActiveTasks(): ListTasksResult {
        return ecsClient.listTasks(ListTasksRequest()
            .withCluster(clusterArn))
    }

    private fun sendSmsAlert(message: String) {

        val request = PublishRequest(smsAlertTopicArn, message)

        try {
            val result = snsClient.publish(request)
            println("Message ${result.messageId} published to $smsAlertTopicArn")
        } catch (e: Exception) {
            println("Exception caught while publishing message to $smsAlertTopicArn")
            e.printStackTrace()
        }
    }

    private fun generateResponse(responseMap: Map<String, String>, statusCode: Int = 200) : APIGatewayProxyResponseEvent {
        val responseBody = Json.encodeToString(responseMap)

        return APIGatewayProxyResponseEvent()
            .withStatusCode(statusCode)
            .withHeaders(mapOf(
                "Access-Control-Allow-Origin" to "*" // TODO properly restrict
            ))
            .withIsBase64Encoded(false)
            .withBody(responseBody)
    }
}