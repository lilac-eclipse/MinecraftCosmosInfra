package com.lilaceclipse.cosmos.client

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.lilaceclipse.cosmos.client.dagger.DaggerCosmosComponent
import com.lilaceclipse.cosmos.client.model.Updater
import io.github.oshai.kotlinlogging.KotlinLogging


fun main(args: Array<String>) {

    val log = KotlinLogging.logger {}

    Updater.finalize(args) // Finalize update, may cause program to exit

    // Needs to happen before any GUI elements are created
    val dark = true
    if (dark) {
        FlatDarkLaf.setup()
    } else {
        FlatLightLaf.setup()
    }

    val cosmosClient = DaggerCosmosComponent.create().provideCosmosClient()
    cosmosClient.run()
}