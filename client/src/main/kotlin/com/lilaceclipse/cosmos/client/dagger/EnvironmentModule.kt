package com.lilaceclipse.cosmos.client.dagger

import com.lilaceclipse.cosmos.docker.config.Environment
import com.lilaceclipse.cosmos.docker.config.EnvironmentConfig
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
                apiUrl = "https://xww3ls66qh.execute-api.us-west-2.amazonaws.com/prod/",
                clientDownloadUrl = "https://cosmos.lilaceclipse.com/Cosmos-Client.jar" // Uses prod for simplicity
            )
            Environment.PROD -> EnvironmentConfig(
                apiUrl = "https://fufgouqjz9.execute-api.us-west-2.amazonaws.com/prod/",
                clientDownloadUrl = "https://cosmos.lilaceclipse.com/Cosmos-Client.jar"
            )
            else -> throw IllegalArgumentException("Invalid launch environment: $environment")
        }
    }
}