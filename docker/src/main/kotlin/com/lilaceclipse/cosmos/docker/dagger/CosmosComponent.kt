package com.lilaceclipse.cosmos.docker.dagger

import com.lilaceclipse.cosmos.docker.daemon.CosmosDaemon
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CosmosModule::class, EnvironmentModule::class])
interface CosmosComponent {
    fun provideCosmosDaemon(): CosmosDaemon
}