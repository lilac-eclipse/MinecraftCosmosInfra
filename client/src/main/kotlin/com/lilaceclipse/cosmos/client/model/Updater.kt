package com.lilaceclipse.cosmos.client.model

import com.lilaceclipse.cosmos.client.view.ClientWindow
import com.lilaceclipse.cosmos.common.CURRENT_CLIENT_VERSION
import com.lilaceclipse.cosmos.common.model.CosmosResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.system.exitProcess


// Prod endpoints are always used for simplicity
const val UPDATE_URL = "https://fufgouqjz9.execute-api.us-west-2.amazonaws.com/prod/"
const val DOWNLOAD_URL = "https://cosmos.lilaceclipse.com/Cosmos-Client.jar"

class Updater @Inject constructor(

) {

    // View bindings
    lateinit var clientWindow: ClientWindow

    // Others
    private val log = KotlinLogging.logger {}
    private val json = Json { ignoreUnknownKeys = true }

    fun checkForUpdates() {
        Thread {
            try {
                val latestVersion = fetchLatestVersion()
                if (isUpdateAvailable(latestVersion)) {
                    log.info { "New version available" }
                    promptForUpdate()
                } else {
                    log.info { "No update available" }
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to check for updates" }
            }
        }.start()
    }

    private fun fetchLatestVersion(): String {
        val requestBody = json.encodeToString(mapOf("requestType" to "CLIENT_VERSION"))

        val connection = URL(UPDATE_URL).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true

            outputStream.use { it.write(requestBody.toByteArray()) }
        }

        val response = connection.inputStream.use { it.bufferedReader().readText() }
        return json.decodeFromString<CosmosResponse.ClientVersionResponse>(response).clientVersion
    }

    private fun isUpdateAvailable(latestVersion: String): Boolean {
        val latestVersionParts = latestVersion.split(".").map(String::toInt)
        val currentVersionParts = CURRENT_CLIENT_VERSION.split(".").map(String::toInt)
        return latestVersionParts.zip(currentVersionParts).any { it.first > it.second }
    }

    private fun promptForUpdate() {
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
    }

    private fun downloadUpdate() {
        Thread {
            try {
                val tempJarFile = File("Cosmos-Client.jar.tmp")
                URL(DOWNLOAD_URL).openStream().use { input ->
                    FileOutputStream(tempJarFile).use { output ->
                        input.copyTo(output)
                    }
                }

                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        clientWindow,
                        "Update downloaded successfully. The application will now restart.",
                        "Update Completed",
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    restartApplication(tempJarFile)
                }
            } catch (e: Exception) {
                log.error(e) { "Failed to download update" }
                SwingUtilities.invokeLater {
                    JOptionPane.showMessageDialog(
                        clientWindow,
                        "Failed to download the update. Please try again later.",
                        "Update Failed",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }
        }.start()
    }

    private fun restartApplication(tempJarFile: File) {
        val javaBin = "${System.getProperty("java.home")}${File.separator}bin${File.separator}java"
        val currentJar = File(Updater::class.java.protectionDomain.codeSource.location.toURI()).path
        val command = arrayOf(javaBin, "-jar", tempJarFile.path, "update-step-1", currentJar)
        Runtime.getRuntime().exec(command)
        exitProcess(0)
    }

    companion object {

        private val log = KotlinLogging.logger {}

        fun finalize(args: Array<String>) {
            if (args.isNotEmpty() && args[0] == "update-step-1") {
                log.info { "This is an updater instance. Completing update step 1."}
                val oldJarPath = args[1]
                val currentJarPath = File(Updater::class.java.protectionDomain.codeSource.location.toURI()).path

                // Wait for the old process to exit and the old JAR to be freed up
                Thread.sleep(1000)

                // Delete the old JAR file
                File(oldJarPath).delete()

                // Copy the new JAR to the old JAR path
                File(currentJarPath).copyTo(File(oldJarPath), overwrite = true)

                // Start the new JAR with the old JAR path
                val javaBin = "${System.getProperty("java.home")}${File.separator}bin${File.separator}java"
                val command = arrayOf(javaBin, "-jar", oldJarPath, "update-step-2", currentJarPath)
                Runtime.getRuntime().exec(command)
                exitProcess(0)
            }

            if (args.isNotEmpty() && args[0] == "update-step-2") {
                log.info { "Finalizing update step 2." }

                val oldJarPath = args[1]

                // Wait for the old process to exit and the old JAR to be freed up
                Thread.sleep(1000)

                // Delete the temporary JAR file
                File(oldJarPath).delete()

                log.info { "Continuing normal execution" }
            }
        }
    }
}