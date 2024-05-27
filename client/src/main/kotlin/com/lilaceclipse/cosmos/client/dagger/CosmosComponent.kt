package com.lilaceclipse.cosmos.client.dagger

import com.lilaceclipse.cosmos.client.CosmosController
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CosmosModule::class, EnvironmentModule::class])
interface CosmosComponent {
    fun provideCosmosClient(): CosmosController
}