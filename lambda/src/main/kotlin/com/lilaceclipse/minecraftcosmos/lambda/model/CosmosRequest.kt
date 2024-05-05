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
    data class StatusRequest(val requestType: String = "STATUS") : CosmosRequest()

    @Serializable
    @SerialName("START")
    data class StartRequest(val requestType: String = "START", val serverUUID: String) : CosmosRequest()
}