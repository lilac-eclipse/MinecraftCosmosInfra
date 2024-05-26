package com.lilaceclipse.cosmos.docker.gameserver

import com.lilaceclipse.cosmos.storage.DynamoStorage
import com.lilaceclipse.cosmos.storage.OnlineStatus
import com.lilaceclipse.cosmos.storage.ServerEntry
import com.lilaceclipse.cosmos.util.ProcessIOAdapter
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Note: This class should only be used once. Once shutdownServer() is called, startServer() should not be called
 * on the same instance.
 */
class MinecraftServerWrapper(
    private val serverRootDir: Path,
    private val dynamoStorage: DynamoStorage
) : com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper {
    private val log = KotlinLogging.logger {}
    private lateinit var minecraftServerProcess: Process
    private lateinit var serverIOAdapter: ProcessIOAdapter
    private var serverStatus = ServerStatus.WAITING

    override fun startServer(launchCommand: String, serverUUID: UUID) {
        log.info { "Starting minecraft server..." }

        val builder = ProcessBuilder()
            .command("sh", "-c", launchCommand)
            .directory(serverRootDir.toFile())
        minecraftServerProcess = builder.start()
        log.info { "Minecraft server process has been started" }
        serverIOAdapter = ProcessIOAdapter(minecraftServerProcess)
        log.info { "Server is entering STARTING state!" }
        serverStatus = ServerStatus.STARTING

        serverIOAdapter.registerRegexWithCallback(Regex(".*Done .*! For help, type.*")){
            log.info { "Server is entering RUNNING state!" }
            serverStatus = ServerStatus.RUNNING

            dynamoStorage.updateServerEntryNonNulls(
                ServerEntry(
                serverId = serverUUID.toString(),
                onlineStatus = OnlineStatus.RUNNING)
            )
        }
        serverIOAdapter.registerRegexWithCallback(Regex(".*FATAL.*Preparing crash report with UUID.*")){
            log.info { "Server is entering ERROR state!" }
            serverStatus = ServerStatus.ERROR
        }
        serverIOAdapter.registerRegexWithCallback(Regex(".*ServerWatchdog.*Considering it to be crashed, server will forcibly shutdown.*")){
            log.info { "Server is entering ERROR state!" }
            serverStatus = ServerStatus.ERROR
        }
    }

    /**
     * Note, this method will block the thread until the MC server is fully closed
     */
    override fun shutdownServer(force: Boolean) {
        if (!minecraftServerProcess.isAlive) {
            cleanup()
            return
        }

        log.info { "Shutting down MC server, force=$force" }
        if (force) {
            minecraftServerProcess.destroyForcibly()
        } else {
            serverIOAdapter.writeLine("stop")
            val bool = minecraftServerProcess.waitFor(30, TimeUnit.SECONDS)
            log.info { "bool=$bool" }
        }

        cleanup()
    }

    private fun cleanup() {
        serverIOAdapter.shutdown()
        log.info { "Server is entering SHUTDOWN state!" }
        serverStatus = ServerStatus.SHUTDOWN
    }

    override fun serverStatus() : ServerStatus {
        return serverStatus
    }

    override fun executeOp() {
        serverIOAdapter.writeLine("op Lilac_Eclipse")
    }

    override fun executeList(): Int? {
        // Pattern is: "[07:53:59] [Server thread/INFO]: There are 0 of a max of 20 players online: "
        val matchResult = serverIOAdapter.writeLineAndGetResponse(
            "list",
            Regex(".*There are ([0-9]+) of a max of [0-9]+ players online.*"))

        return matchResult?.destructured?.toList()?.get(0)?.toIntOrNull()
    }
}