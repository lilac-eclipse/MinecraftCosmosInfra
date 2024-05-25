package com.lilaceclipse.cosmos.storage

import com.amazonaws.services.s3.transfer.TransferManager
import com.lilaceclipse.cosmos.config.EnvironmentConfig
import com.lilaceclipse.cosmos.util.FilePrintUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject

class S3Storage @Inject constructor(
    private val transferManager: TransferManager,
    environmentConfig: EnvironmentConfig
) {

    private val log = KotlinLogging.logger {}

    private val serverS3Bucket = environmentConfig.s3Bucket
    private val serverS3KeyPrefix = "servers/"
    private val tmpStorageDir = Paths.get("tmp/cosmos/gameserver/download/")

    private val foldersToExcludeFromUpload = listOf(
        Paths.get("logs"), // Logs are in cloudwatch
        Paths.get("mods"), // Unchanged between runs
        Paths.get("libraries"), // Unchanged between runs
        Paths.get(".fabric"), // Unchanged between runs
        Paths.get("pfm"), // Mainly a cache directory, has many objects which leads to lengthy downloads/uploads
        Paths.get("versions") // Unchanged between runs
    )

    fun downloadMinecraft(storageDir: Path, serverS3KeySuffix: String) {
        log.info { "Starting download" }
        val serverS3Key = serverS3KeyPrefix + serverS3KeySuffix
        val download = transferManager.downloadDirectory(
            serverS3Bucket, serverS3Key, tmpStorageDir.toFile())

        download.waitForCompletion()
        log.info { "Download complete" }
        
        // Directory is placed embedded in the intended storage dir, need to move it one up
        FileUtils.moveDirectory(File("$tmpStorageDir/$serverS3Key"), storageDir.toFile())
    }

    fun uploadMinecraft(storageDir: Path, serverS3KeySuffix: String) {
        log.info { "Starting upload" }
        val serverS3Key = serverS3KeyPrefix + serverS3KeySuffix

        // Delete re-creatable files before upload
        foldersToExcludeFromUpload.forEach { FileUtils.deleteDirectory(storageDir.resolve(it).toFile()) }

        val upload = transferManager.uploadDirectory(
            serverS3Bucket, serverS3Key, storageDir.toFile(), true)

        upload.waitForCompletion()
        log.info { "Upload complete" }
    }
}