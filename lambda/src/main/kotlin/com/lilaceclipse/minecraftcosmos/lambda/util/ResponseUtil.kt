package com.lilaceclipse.minecraftcosmos.lambda.util

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ResponseUtil @Inject constructor() {

    fun generateResponse(responseMap: Map<String, String>, statusCode: Int = 200): APIGatewayProxyResponseEvent {
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