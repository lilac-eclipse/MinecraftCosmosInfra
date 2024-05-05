package com.lilaceclipse.minecraftcosmos.lambda.util

import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest
import kotlinx.serialization.json.Json
import javax.inject.Inject

class RequestUtil @Inject constructor() {
    fun parseRequest(requestBody: String): CosmosRequest {
        return try {
            Json.decodeFromString<CosmosRequest>(requestBody)
        } catch (e: Exception) {
            println("Failed to parse request body: $requestBody")
            throw IllegalArgumentException("Failed to parse request body: $requestBody", e)
        }
    }
}