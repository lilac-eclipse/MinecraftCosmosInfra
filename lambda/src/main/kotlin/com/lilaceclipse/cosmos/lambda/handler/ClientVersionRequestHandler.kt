package com.lilaceclipse.cosmos.lambda.handler

import com.lilaceclipse.cosmos.common.CURRENT_CLIENT_VERSION
import com.lilaceclipse.cosmos.common.model.CosmosRequest.ClientVersionRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.ClientVersionResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

class ClientVersionRequestHandler @Inject constructor(

) {
    private val log = KotlinLogging.logger {}

    fun handleRequest(request: ClientVersionRequest): CosmosResponse {

        return ClientVersionResponse(
            clientVersion = CURRENT_CLIENT_VERSION
        )
    }
}