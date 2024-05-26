package com.lilaceclipse.cosmos.client

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.lilaceclipse.cosmos.client.dagger.DaggerCosmosComponent


fun main() {

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