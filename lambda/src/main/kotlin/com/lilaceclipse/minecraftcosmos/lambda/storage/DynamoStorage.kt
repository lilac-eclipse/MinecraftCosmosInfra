package com.lilaceclipse.minecraftcosmos.lambda.storage


import com.lilaceclipse.minecraftcosmos.lambda.model.ServerEntry
import com.lilaceclipse.minecraftcosmos.lambda.util.EnvVarProvider
import mu.KotlinLogging
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import javax.inject.Inject


class DynamoStorage @Inject constructor(
    enhancedClient: DynamoDbEnhancedClient,
    envVarProvider: EnvVarProvider
) {

    private val log = KotlinLogging.logger {}

    private val serverTableSchema = TableSchema.fromBean(ServerEntry::class.java)
    private val serverTable = enhancedClient.table(envVarProvider.serverTableName, serverTableSchema)

    fun getServerEntryFromDb(serverId: String) : ServerEntry {
        val serverEntry = serverTable.getItem(
            Key.builder()
                .partitionValue(serverId)
                .build())

        return serverEntry
    }

    fun getActiveServerEntries(): List<ServerEntry> {
        val filterExpression = Expression.builder()
            .expression("serverState = :state")
            .putExpressionValue(":state", AttributeValue.builder().s("ACTIVE").build())
            .build()

        val scanRequest = ScanEnhancedRequest.builder()
            .filterExpression(filterExpression)
            .consistentRead(false)
            .build()

        val results = serverTable.scan(scanRequest)
        val serverEntries = results.flatMap { page -> page.items() }

        return serverEntries
    }
}