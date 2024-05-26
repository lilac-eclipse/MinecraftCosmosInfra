package com.lilaceclipse.cosmos.docker.dagger

import com.lilaceclipse.cosmos.config.Environment
import com.lilaceclipse.cosmos.daemon.CosmosCli
import com.lilaceclipse.cosmos.daemon.CosmosDaemon
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [CosmosModule::class, EnvironmentModule::class])
interface CosmosComponent {
    fun provideCosmosDaemon(): CosmosDaemon
}