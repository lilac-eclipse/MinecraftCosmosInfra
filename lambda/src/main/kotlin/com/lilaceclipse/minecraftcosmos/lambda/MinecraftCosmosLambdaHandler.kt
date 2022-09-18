package com.lilaceclipse.minecraftcosmos.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.AmazonSNSClientBuilder
import com.amazonaws.services.sns.model.PublishRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


/**
 * See: https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html
 */
class MinecraftCosmosLambdaHandler: RequestHandler<Map<String, Any>, APIGatewayProxyResponseEvent> {

    private val STATUS_ALERT_TOPIC_ENV_VAR = "STATUS_ALERT_TOPIC_ARN"

    private val snsClient: AmazonSNS = AmazonSNSClientBuilder.defaultClient()

    override fun handleRequest(input: Map<String, Any>?, context: Context?): APIGatewayProxyResponseEvent {
        // TODO - achieve parity with current implementation (invoke ec2)
        //  X setup prod/beta stacks
        //  deploy prod stack and validate functionality
        //  deprecate old code/packages + upload this to github
        //  ===
        //  setup new fargate/docker implementation

        val arn = System.getenv(STATUS_ALERT_TOPIC_ENV_VAR)
        println("using messaging arn $arn")

        val request = PublishRequest(
            arn,
            "test message, ignore this :o")

        try {
            val result = snsClient.publish(request)
            println("Message ${result.messageId} published")
        } catch (e: Exception) {
            println("Exception caught while publishing message")
            e.printStackTrace()
        }

        val responseBody = Json.encodeToString(mapOf(
            "message" to "Started cosmos!"
        ))

        return APIGatewayProxyResponseEvent()
            .withStatusCode(200)
            .withIsBase64Encoded(false)
            .withBody(responseBody)

    }
}