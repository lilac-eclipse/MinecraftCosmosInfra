package com.lilaceclipse.minecraftcosmos.lambda

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.StartInstancesRequest
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

    private val STATUS_ALERT_TOPIC_ENV_VAR = "STATUS_ALERT_TOPIC_ARN"
    private val EC2_INSTANCE_ID = "i-021869b2f1e4e523d"


    private val snsClient: AmazonSNS = AmazonSNSClientBuilder.defaultClient()
    private val ec2Client: AmazonEC2 = AmazonEC2ClientBuilder.defaultClient()
    private val smsAlertTopicArn = System.getenv(STATUS_ALERT_TOPIC_ENV_VAR)

    override fun handleRequest(input: Map<String, Any>, context: Context): APIGatewayProxyResponseEvent {
        var requestBody: Map<String, String>? = null
        try {
            requestBody = Json.decodeFromString(input["body"] as String)
        } catch (e: Exception) {
            println("Failed to decode input body from: ${input["body"]}")
        }

        return if (requestBody?.get("requestType") == "STATUS") {
            handleStatusRequest(requestBody)
        } else if (requestBody?.get("requestType") == "START") {
            handleStartRequest(requestBody)
        } else {
            println(input)
            generateResponse(mapOf(
                "message" to "Request type not supported"
            ))
        }
    }

    private fun handleStatusRequest(requestBody: Map<String, String>) : APIGatewayProxyResponseEvent {
        return generateResponse(mapOf(
            "status" to "RUNNING",
            "ip" to "10.23.1.421"
        ))
    }

    private fun handleStartRequest(requestBody: Map<String, String>) : APIGatewayProxyResponseEvent {
        val instance = ec2Client.describeInstances(
            DescribeInstancesRequest()
                .withInstanceIds(EC2_INSTANCE_ID))
            .reservations[0]
            .instances[0]

        return if (listOf("pending", "running").contains(instance.state.name)) {
            println("Received request to start service, but it was already running")
            generateResponse(mapOf(
                "message" to "Cosmos was already started :)"
            ))
        } else {
            println("Received request to start service, will now attempt to start")
            startEc2InstanceAndReturnResponse()
        }
    }

    private fun startEc2InstanceAndReturnResponse() : APIGatewayProxyResponseEvent {
        ec2Client.startInstances(StartInstancesRequest()
            .withInstanceIds(EC2_INSTANCE_ID))

        sendSmsAlert("Cosmos has started! Join at mc.cryo3.net")
        return generateResponse(
            mapOf(
            "message" to "Started cosmos! Automated messaging is currently unavailable, " +
                    "please alert people accordingly :)")
        )
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