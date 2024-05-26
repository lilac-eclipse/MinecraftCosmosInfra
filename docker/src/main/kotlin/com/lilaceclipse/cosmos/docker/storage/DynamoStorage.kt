package com.lilaceclipse.cosmos.docker.storage

import com.lilaceclipse.cosmos.common.model.ServerEntry
import com.lilaceclipse.cosmos.docker.config.EnvironmentConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import javax.inject.Inject


class DynamoStorage @Inject constructor(
    enhancedClient: DynamoDbEnhancedClient,
    environmentConfig: EnvironmentConfig
) {

    private val log = KotlinLogging.logger {}

    private val serverTableSchema = TableSchema.fromBean(ServerEntry::class.java)
    private val serverTable = enhancedClient.table(environmentConfig.dynamoDbTableName, serverTableSchema)

    fun getServerEntryFromDb(serverId: String) : ServerEntry {
        val serverEntry = serverTable.getItem(
            Key.builder()
                .partitionValue(serverId)
                .build())

        return serverEntry
    }

    fun updateServerEntryNonNulls(serverEntry: ServerEntry) {
        val updateItemRequest = UpdateItemEnhancedRequest.builder(ServerEntry::class.java)
            .item(serverEntry)
            .ignoreNulls(true)
            .build()

        try {
            serverTable.updateItem(updateItemRequest)
            log.info { "Updated server entry $serverEntry" }
        } catch (e: Exception) {
            log.error(e) { "Failed to update server entry for request $serverEntry" }
            throw e
        }
    }
}