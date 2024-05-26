package com.lilaceclipse.cosmos.client

import com.lilaceclipse.cosmos.client.model.ModInstaller
import com.lilaceclipse.cosmos.client.model.Updater
import com.lilaceclipse.cosmos.client.view.ClientWindow
import javax.inject.Inject
import javax.swing.SwingUtilities

class CosmosController @Inject constructor(
    private val clientWindow: ClientWindow,
    private val modInstaller: ModInstaller,
    private val updater: Updater,
) {
    fun run() {
        bindModelsWithViews()

        SwingUtilities.invokeLater {
            clientWindow.isVisible = true
            updater.checkForUpdates()
        }
    }

    private fun bindModelsWithViews() {
        modInstaller.clientWindow = clientWindow
        clientWindow.modInstaller = modInstaller

        updater.clientWindow = clientWindow
        // no reverse binding for updater
    }
}
