package com.lilaceclipse.cosmos.client.model

import java.io.File
import javax.inject.Inject

class FileUtil @Inject constructor() {

    fun getDefaultInstallDirectory(): File {
        val os = System.getProperty("os.name").toLowerCase()
        val appDataDir = when {
            os.contains("win") -> File(System.getenv("APPDATA"))
            os.contains("mac") -> File(System.getProperty("user.home"), "/Library/Application Support")
            else -> File(System.getProperty("user.home"))
        }
        return File(appDataDir, ".minecraft")
    }
}