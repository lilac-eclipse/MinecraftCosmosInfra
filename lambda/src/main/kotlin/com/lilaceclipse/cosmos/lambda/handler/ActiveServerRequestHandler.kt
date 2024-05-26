package com.lilaceclipse.cosmos.lambda.handler

import com.lilaceclipse.cosmos.common.model.CosmosRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.ActiveServerResponse
import com.lilaceclipse.cosmos.lambda.storage.DynamoStorage
import io.github.oshai.kotlinlogging.KotlinLogging
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