package com.lilaceclipse.minecraftcosmos.lambda.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("requestType")
sealed class CosmosRequest {
    @Serializable
    @SerialName("STATUS")
    data class StatusRequest(
        val requestType: String
    ) : CosmosRequest()

    @Serializable
    @SerialName("START")
    data class StartRequest(
        val requestType: String,
        val serverUUID: String
    ) : CosmosRequest()

    @Serializable
    @SerialName("SERVERS")
    data class ActiveServerRequest(
        val requestType: String
    ) : CosmosRequest()
}