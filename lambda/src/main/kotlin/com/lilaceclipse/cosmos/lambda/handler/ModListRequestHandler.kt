package com.lilaceclipse.cosmos.lambda.handler

import com.lilaceclipse.cosmos.common.model.CosmosRequest.ModListRequest
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import com.lilaceclipse.cosmos.common.model.CosmosResponse.ModListResponse
import com.lilaceclipse.cosmos.common.model.ModReference
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject

class ModListRequestHandler @Inject constructor(

) {

    private val log = KotlinLogging.logger {}

    fun handleRequest(request: ModListRequest): CosmosResponse {

        request.serverUUID
        return ModListResponse(
            modList = listOf(
                ModReference("abc")
            )
        )
    }
}