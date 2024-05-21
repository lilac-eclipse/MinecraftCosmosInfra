package com.lilaceclipse.minecraftcosmos.lambda.util

import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest
import kotlinx.serialization.json.Json
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

class RequestUtil @Inject constructor() {
    private val log = KotlinLogging.logger {}

    fun parseRequest(requestBody: String): CosmosRequest {
        return try {
            Json.decodeFromString<CosmosRequest>(requestBody)
        } catch (e: Exception) {
            log.info { "Failed to parse request body: $requestBody" }
            throw IllegalArgumentException("Failed to parse request body: $requestBody", e)
        }
    }
}