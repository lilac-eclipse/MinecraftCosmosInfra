package com.lilaceclipse.cosmos.lambda.dagger

import com.lilaceclipse.cosmos.lambda.handler.CosmosRequestRouter
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CosmosModule::class])
interface CosmosComponent {
    fun provideCosmosRequestRouter(): CosmosRequestRouter
}