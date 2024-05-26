package com.lilaceclipse.cosmos.client.model

import com.lilaceclipse.cosmos.client.view.ClientWindow
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

const val CURRENT_CLIENT_VERSION = "0.1.4.0"
const val UPDATE_URL = "https://xww3ls66qh.execute-api.us-west-2.amazonaws.com/prod/"
const val DOWNLOAD_URL = "https://cosmos.lilaceclipse.com/client.jar"

class Updater @Inject constructor(

) {

    // View bindings
    lateinit var clientWindow: ClientWindow

    // Others
    private val log = KotlinLogging.logger {}

    fun checkForUpdates() {
        val updateThread = Thread {
            try {
                val connection = URL(UPDATE_URL).openConnection()
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                val outputStream = connection.getOutputStream()
                outputStream.write("""{"requestType":"CLIENT_VERSION"}""".toByteArray())
                outputStream.flush()
                outputStream.close()

                val inputStream = connection.getInputStream()
                val response = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val json = Json { ignoreUnknownKeys=true }
                val clientVersionResponse = json.decodeFromString<CosmosResponse.ClientVersionResponse>(response)
                val latestVersion = clientVersionResponse.clientVersion

                log.info { "latestVersion=$latestVersion" }

                if (latestVersion > CURRENT_CLIENT_VERSION) {
                    log.info { "New version available" }
                    SwingUtilities.invokeLater {
                        val result = JOptionPane.showConfirmDialog(
                            clientWindow,
                            "A new version of Cosmos Installer is available. Do you want to update now?",
                            "Update Available",
                            JOptionPane.YES_NO_OPTION
                        )
                        if (result == JOptionPane.YES_OPTION) {
                            downloadUpdate()
                        }
                    }
                } else {
                    log.info { "No update available" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        updateThread.start()
    }

    private fun downloadUpdate() {
        val downloadThread = Thread {
            try {
                val jarFileName = "cosmos-installer.jar"
                val tempJarFile = File(jarFileName + ".tmp")
                val outputStream = FileOutputStream(tempJarFile)
                val inputStream = URL(DOWNLOAD_URL).openStream()
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()

                val currentJarFile = File(jarFileName)
                if (currentJarFile.exists()) {
                    currentJarFile.delete()
                }
                tempJarFile.renameTo(currentJarFile)

                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        clientWindow,
                        "Update downloaded successfully. The application will now restart.",
                        "Update Completed",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    restartApplication()
                }
            } catch (e: Exception) {
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        clientWindow,
                        "Failed to download the update. Please try again later.",
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
                e.printStackTrace()
            }
        }
        downloadThread.start()
    }

    private fun restartApplication() {
        val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
        val currentJar = File(ClientWindow::class.java.protectionDomain.codeSource.location.toURI()).path
        val command = arrayOf(javaBin, "-jar", currentJar)
        Runtime.getRuntime().exec(command)
        exitProcess(0)
    }
}