package com.lilaceclipse.minecraftcosmos.lambda.handler

import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosRequest
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse
import com.lilaceclipse.minecraftcosmos.lambda.model.CosmosResponse.ActiveServerResponse
import com.lilaceclipse.minecraftcosmos.lambda.storage.DynamoStorage
import mu.KotlinLogging
import javax.inject.Inject

class ActiveServerRequestHandler @Inject constructor(
    private val dynamoStorage: DynamoStorage
) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: CosmosRequest.ActiveServerRequest): CosmosResponse {
        val serverEntries = dynamoStorage.getActiveServerEntries()

        log.info { serverEntries }

        return ActiveServerResponse(
            servers = serverEntries
        )
    }
}