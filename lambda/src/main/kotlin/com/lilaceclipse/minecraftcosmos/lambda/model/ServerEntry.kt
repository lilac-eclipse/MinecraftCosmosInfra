package com.lilaceclipse.minecraftcosmos.lambda.model

import kotlinx.serialization.Serializable
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

@DynamoDbBean
@Serializable
data class ServerEntry(
    @get:DynamoDbPartitionKey
    var serverId: String? = null, // UUIDv4
    var serverName: String? = null, // User-friendly name of the server
    @get:DynamoDbConvertedBy(ServerStateConverter::class)
    var serverState: ServerState? = null, // "Active" or "Archived"
    @get:DynamoDbConvertedBy(OnlineStatusConverter::class)
    var onlineStatus: OnlineStatus? = null, // "Online" or "Offline"
    var s3KeySuffix: String? = null, // S3 location of server files
    var launchCommand: String? = null // Launch command
)

class ServerStateConverter : AttributeConverter<ServerState> {
    override fun transformFrom(input: ServerState): AttributeValue {
        return AttributeValue.builder().s(input.name).build()
    }

    override fun transformTo(input: AttributeValue): ServerState {
        return ServerState.valueOf(input.s())
    }

    override fun type(): EnhancedType<ServerState> {
        return EnhancedType.of(ServerState::class.java)
    }

    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.S
    }
}
class OnlineStatusConverter : AttributeConverter<OnlineStatus> {
    override fun transformFrom(input: OnlineStatus): AttributeValue {
        return AttributeValue.builder().s(input.name).build()
    }

    override fun transformTo(input: AttributeValue): OnlineStatus {
        return OnlineStatus.valueOf(input.s())
    }

    override fun type(): EnhancedType<OnlineStatus> {
        return EnhancedType.of(OnlineStatus::class.java)
    }

    override fun attributeValueType(): AttributeValueType {
        return AttributeValueType.S
    }
}