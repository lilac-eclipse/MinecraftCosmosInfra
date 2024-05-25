package com.lilaceclipse.cosmos.dagger

import com.lilaceclipse.cosmos.config.Environment
import com.lilaceclipse.cosmos.config.EnvironmentConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class EnvironmentModule(private val environment: Environment) {

    @Provides
    @Singleton
    fun provideEnvironmentConfig(): EnvironmentConfig {
        return when (environment) {
            Environment.BETA -> EnvironmentConfig(
                dynamoDbTableName = "CosmosServers-beta",
                s3Bucket = "mccosmos-data-beta"
            )
            Environment.PROD -> EnvironmentConfig(
                dynamoDbTableName = "CosmosServers-prod",
                s3Bucket = "mccosmos-data-prod"
            )
            else -> throw IllegalArgumentException("Invalid launch environment: $environment")
        }
    }
}