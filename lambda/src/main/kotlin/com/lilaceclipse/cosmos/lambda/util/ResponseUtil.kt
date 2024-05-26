package com.lilaceclipse.cosmos.lambda.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.lilaceclipse.cosmos.lambda.model.CosmosResponse
import com.lilaceclipse.cosmos.lambda.model.CosmosResponse.ClientErrorResponse
import com.lilaceclipse.cosmos.lambda.model.CosmosResponse.ServerErrorResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ResponseUtil @Inject constructor() {

    fun generateResponse(response: CosmosResponse): APIGatewayProxyResponseEvent {

        val statusCode = when (response) {
            is ClientErrorResponse -> 400
            is ServerErrorResponse -> 500
            else -> 200
        }
        return APIGatewayProxyResponseEvent()
            .withStatusCode(statusCode)
            .withHeaders(mapOf(
                "Access-Control-Allow-Origin" to "*" // TODO properly restrict
            ))
            .withIsBase64Encoded(false)
            .withBody(Json.encodeToString(response))
    }
}