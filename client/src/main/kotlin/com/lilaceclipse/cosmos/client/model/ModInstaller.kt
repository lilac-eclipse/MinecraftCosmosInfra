package com.lilaceclipse.cosmos.client.model

import com.lilaceclipse.cosmos.client.view.ClientWindow
import io.github.oshai.kotlinlogging.KotlinLogging
import javax.inject.Inject
import javax.swing.JOptionPane

class ModInstaller @Inject constructor(

) {
    // View bindings
    lateinit var clientWindow: ClientWindow

    // Others
    private val log = KotlinLogging.logger {}

    fun installMods() {
        // TODO: Implement mod installation logic
        JOptionPane.showMessageDialog(clientWindow, "Mod installation not implemented yet.")
    }
}