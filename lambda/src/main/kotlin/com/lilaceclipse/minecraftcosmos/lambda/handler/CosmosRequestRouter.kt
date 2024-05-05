package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest
import com.lilaceclipse.minecraftcosmos.lambda.util.RequestUtil
import com.lilaceclipse.minecraftcosmos.lambda.util.ResponseUtil
import javax.inject.Inject

class CosmosRequestRouter @Inject constructor(
    private val statusRequestHandler: StatusRequestHandler,
    private val startRequestHandler: StartRequestHandler,
    private val requestUtil: RequestUtil,
    private val responseUtil: ResponseUtil
) {

    fun handleRequest(input: Map<String, Any>): APIGatewayProxyResponseEvent {
        println("Received Request")
        val requestBody = input["body"] as? String
        val cosmosRequest = requestBody?.let { requestUtil.parseRequest(it) }

        return when (cosmosRequest) {
            is CosmosRequest.StatusRequest -> {
                statusRequestHandler.handleRequest(cosmosRequest)
            }
            is CosmosRequest.StartRequest -> {
                startRequestHandler.handleRequest(cosmosRequest)
            }
            else -> {
                println(input)
                responseUtil.generateResponse(mapOf(
                    "message" to "Invalid request body or unsupported request type"
                ))
            }
        }
    }

}