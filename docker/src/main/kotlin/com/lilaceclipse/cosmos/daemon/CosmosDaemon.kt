package com.lilaceclipse.cosmos.daemon

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Main Daemon which should be launched at the beginning of execution.
 */
class CosmosDaemon @Inject constructor(
    private val gameServerLifecycleManagerFactory: GameServerLifecycleManagerFactory,
    private val cosmosRouteHandlerFactory: CosmosRouteHandlerFactory,
    private val taskSchedulerFactory: TaskSchedulerFactory
) {

    private val log = KotlinLogging.logger {}

    private lateinit var gameServerLifecycleManager: GameServerLifecycleManager
    private lateinit var cosmosRouteHandler: CosmosRouteHandler
    private lateinit var taskScheduler: TaskScheduler

    private lateinit var webServerJob: Job
    private lateinit var gameServerJob: Job
    private lateinit var taskSchedulerJob: Job

    private lateinit var webServer: ApplicationEngine
    private val isShuttingDown: AtomicBoolean = AtomicBoolean(false)

    @OptIn(DelicateCoroutinesApi::class)
    fun run(targetServerUuid: UUID) = runBlocking {

        gameServerLifecycleManager = gameServerLifecycleManagerFactory.create(targetServerUuid)
        cosmosRouteHandler = cosmosRouteHandlerFactory.create(gameServerLifecycleManager, this@CosmosDaemon)
        taskScheduler = taskSchedulerFactory.create(gameServerLifecycleManager, this@CosmosDaemon)

        webServerJob = launch(newSingleThreadContext("ktor-thread")) {
            webServer = embeddedServer(Netty, port = 80) {
                routing {
                    // Health check executed in regular intervals by docker (1m at time of writing)
                    get("/health") { cosmosRouteHandler.handleGetHealth(call) }
                    get("/status") { cosmosRouteHandler.handleGetStatus(call) }
                    get("/stop") { cosmosRouteHandler.handleGetStop(call) }
                    get("/list") { cosmosRouteHandler.handleGetList(call) }
                    get("/op") { cosmosRouteHandler.handleGetOp(call) }
                }
            }
            webServer.start(wait = true)
            log.info { "Web server coroutine complete" }
        }

        // TODO prevent this from exiting until the server is shutdown
        gameServerJob = launch {
            gameServerLifecycleManager.runServer()
            log.info { "Game server coroutine complete" }
        }

        // TODO prevent this from exiting until no more task scheduler jobs are running
        taskSchedulerJob = launch {
            // cancelling this coroutine scope will propagate the cancellation to child scopes
            taskScheduler.startUpdateDatabaseStatusEveryMinute(this, targetServerUuid)
            taskScheduler.startUpdatePlayerArrayEveryMinute(this)
            log.info { "Task scheduler coroutine complete" }
        }
    }

    suspend fun shutdown(force: Boolean) {
        log.info { "Shutting down TaskScheduler" }
        if (isShuttingDown.getAndSet(true)) {
            log.info { "Server already shutting down, skipping" }
            return
        }
        log.info { "This is the first shutdown, continuing" }

        taskSchedulerJob.cancel()

        log.info { "Shutting down gameServerLifecycleManager, force=$force" }
        gameServerLifecycleManager.stopServer(force)


        // Ok this is a silly way to stop the webserver but for some reason webServer.stop() hangs
        //  so this is my nonsense solution to fix it
        log.info { "Shutting down webServer" }

        // First, stop the server in a way that i think is gracefully. this is copied from the ktor ShutDownUrl
        //  plugin code doShutdown(). this doesn't actually unblock the thread though, just shuts down the server
        val environment = webServer.environment
        environment.monitor.raise(ApplicationStopPreparing, environment)
        environment.stop()

        // Second, cancel the coroutine that the webserver is running. for some reason this causes the thread to
        //  unblock and continue running. this will allow the coroutine to complete, which in turn allows runBlocking
        //  to exit
        webServerJob.cancelAndJoin()

        log.info { "Shutdown complete" }
    }
}
