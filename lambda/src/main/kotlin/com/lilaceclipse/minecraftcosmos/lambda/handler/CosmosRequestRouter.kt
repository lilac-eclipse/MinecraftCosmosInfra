package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse.ClientErrorResponse
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse.ServerErrorResponse
import com.lilaceclipse.minecraftcosmos.lambda.util.RequestUtil
import com.lilaceclipse.minecraftcosmos.lambda.util.ResponseUtil
import mu.KotlinLogging
import javax.inject.Inject

class CosmosRequestRouter @Inject constructor(
    private val statusRequestHandler: StatusRequestHandler,
    private val startRequestHandler: StartRequestHandler,
    private val activeServerRequestHandler: ActiveServerRequestHandler,
    private val requestUtil: RequestUtil,
    private val responseUtil: ResponseUtil
) {

    private val log = KotlinLogging.logger {}

    fun handleRequest(input: Map<String, Any>): APIGatewayProxyResponseEvent {
        log.info { "Request received: $input" }
        val requestBody = input["body"] as String

        val response = try {
            val cosmosRequest = requestUtil.parseRequest(requestBody)
            when (cosmosRequest) {
                is CosmosRequest.StatusRequest -> {
                    statusRequestHandler.handleRequest(cosmosRequest)
                }
                is CosmosRequest.StartRequest -> {
                    startRequestHandler.handleRequest(cosmosRequest)
                }
                is CosmosRequest.ActiveServerRequest -> {
                    activeServerRequestHandler.handleRequest(cosmosRequest)
                }
            }
        } catch (e: IllegalArgumentException) {
            log.error { "Malformed request body" }
            e.printStackTrace()
            ClientErrorResponse("Malformed request body")
        } catch (e: Exception) {
            log.error { "An unknown error occurred" }
            e.printStackTrace()
            ServerErrorResponse("Something went wrong on server side... Please notify us :D")
        }

        log.info { "Request handled" }
        return responseUtil.generateResponse(response)
    }

}