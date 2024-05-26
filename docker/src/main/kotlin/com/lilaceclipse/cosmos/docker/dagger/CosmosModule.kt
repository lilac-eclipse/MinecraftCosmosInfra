package com.lilaceclipse.cosmos.docker.dagger

import com.amazonaws.services.ec2.AmazonEC2
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder
import com.amazonaws.services.ecs.AmazonECS
import com.amazonaws.services.ecs.AmazonECSClientBuilder
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import dagger.Module
import dagger.Provides
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

@Module
class CosmosModule {

    @Provides
    fun provideTransferManager() : TransferManager {
        return TransferManagerBuilder.defaultTransferManager()
    }

    @Provides
    fun provideDynamoDbEnhancedClient(): DynamoDbEnhancedClient {
        return DynamoDbEnhancedClient.create()
    }

    @Provides
    fun provideAmazonEC2():  AmazonEC2 {
        return AmazonEC2ClientBuilder.defaultClient()
    }

    @Provides
    fun provideAmazonECS(): AmazonECS {
        return AmazonECSClientBuilder.defaultClient()
    }
}
