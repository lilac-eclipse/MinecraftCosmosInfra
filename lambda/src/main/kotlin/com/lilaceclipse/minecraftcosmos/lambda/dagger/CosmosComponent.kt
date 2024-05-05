package com.lilaceclipse.minecraftcosmos.lambda.dagger

import com.lilaceclipse.minecraftcosmos.lambda.handler.CosmosRequestRouter
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CosmosModule::class])
interface CosmosComponent {
    fun provideCosmosRequestRouter(): CosmosRequestRouter
}