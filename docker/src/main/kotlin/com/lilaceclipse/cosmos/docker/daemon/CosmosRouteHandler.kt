package com.lilaceclipse.cosmos.docker.daemon

import com.lilaceclipse.cosmos.docker.gameserver.ServerStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.withTimeoutOrNull
import io.github.oshai.kotlinlogging.KotlinLogging

class CosmosRouteHandler @AssistedInject constructor(
    @Assisted private val gameServerLifecycleManager: GameServerLifecycleManager,
    @Assisted private val cosmosDaemon: CosmosDaemon
) {
    private val log = KotlinLogging.logger {}

    private val serverWrapper: com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper? get() = gameServerLifecycleManager.gameServerWrapper

    suspend fun handleGetHealth(call: ApplicationCall) {
        log.info { "Received call to getHealth" }
        call.respondText("Healthy!")

        // TODO set gameServerWrapper to something before download is complete, or update how status checking
        //  works in route handler/task scheduler, otherwise there's a possibility that they detect null
        //  and abort early
        val numPlayers = if (serverWrapper?.serverStatus() == ServerStatus.STARTING) {
            0 // Simulate 0 players online if the server is starting. This will cause shutdown after 30min
        } else {
            withTimeoutOrNull(30000L) { serverWrapper?.executeList() }
        }

        if (numPlayers == null) {
            log.error { "List command timed out, forcing server shutdown" }
            cosmosDaemon.shutdown(true)
        }
    }

    suspend fun handleGetStatus(call: ApplicationCall) {
        log.info { "Received call to getStatus" }

        // TODO actually get server status for status check
        if (serverWrapper == null) {
            call.respondText("Server is not started")
        } else {
            call.respondText("Server is running")
        }
    }

    suspend fun handleGetStop(call: ApplicationCall) {
        log.info { "Received call to getStop" }
        call.respondText("Stopping server")
        cosmosDaemon.shutdown(false)
    }

    suspend fun handleGetList(call: ApplicationCall) {
        log.info { "Received call to getList" }
        if (serverWrapper == null) {
            call.respondText("Server is not started")
        } else {
            call.respondText("Server command sent")
            log.info { "Starting call" }
            val numPlayers = withTimeoutOrNull(30000L) {
                serverWrapper?.executeList()
            }
            log.info { "$numPlayers players online" }
        }
    }

    suspend fun handleGetOp(call: ApplicationCall) {
        log.info { "Received call to getOp" }
        if (serverWrapper == null) {
            call.respondText("Server is not started")
        } else {
            call.respondText("Op sent to server, see cloudwatch for logs")
            serverWrapper?.executeOp()
        }
    }
}

@AssistedFactory
interface CosmosRouteHandlerFactory {
    fun create(
        gameServerLifecycleManager: GameServerLifecycleManager,
        cosmosDaemon: CosmosDaemon
    ): CosmosRouteHandler
}