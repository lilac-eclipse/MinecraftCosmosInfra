package com.lilaceclipse.cosmos.common.model

import kotlinx.serialization.Serializable

@Serializable
sealed class CosmosResponse {
    @Serializable
    data class StatusResponse(
        val status: String,
        val ip: String?
    ) : CosmosResponse()

    @Serializable
    data class StartResponse(
        val message: String
    ) : CosmosResponse()

    @Serializable
    data class ActiveServerResponse(
        val servers: List<ServerEntry>
    ) : CosmosResponse()

    @Serializable
    data class ModListResponse(
        val modList: List<ModReference>
    ) : CosmosResponse()

    @Serializable
    data class ClientVersionResponse(
        val clientVersion: String
    ) : CosmosResponse()

    @Serializable
    data class ClientErrorResponse(
        val message: String
    ) : CosmosResponse()

    @Serializable
    data class ServerErrorResponse(
        val message: String
    ) : CosmosResponse()
}