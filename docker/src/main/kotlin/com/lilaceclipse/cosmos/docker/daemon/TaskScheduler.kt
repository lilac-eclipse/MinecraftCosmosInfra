package com.lilaceclipse.cosmos.docker.daemon

import com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper
import com.lilaceclipse.cosmos.gameserver.ServerStatus
import com.lilaceclipse.cosmos.storage.DynamoStorage
import com.lilaceclipse.cosmos.storage.OnlineStatus
import com.lilaceclipse.cosmos.storage.ServerEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

class TaskScheduler @AssistedInject constructor(
    private val dynamoStorage: DynamoStorage,
    @Assisted private val gameServerLifecycleManager: GameServerLifecycleManager,
    @Assisted private val cosmosDaemon: CosmosDaemon
) {
    private val log = KotlinLogging.logger {}

    private val serverWrapper: com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper? get() = gameServerLifecycleManager.gameServerWrapper
    private val activePlayerDeque = ArrayDeque<Int>(35)

    suspend fun startUpdatePlayerArrayEveryMinute(scope: CoroutineScope) {
        scope.launch {
            for (i in 0 until 30) {
                // Pre-populate the deque so the server stays on for a minimum duration
                activePlayerDeque.addFirst(1)
            }

            while (true) {
                delay(60000)

                val numPlayers = if (serverWrapper?.serverStatus() == ServerStatus.STARTING) {
                    0 // Simulate 0 players online if the server is starting. This will cause shutdown after 30min
                } else {
                    withTimeoutOrNull(30000L) { serverWrapper?.executeList() }
                }

                activePlayerDeque.removeLast()
                activePlayerDeque.addFirst(numPlayers ?: 0)
                log.info { "Shutdown score is: ${activePlayerDeque.sum()}. Will shutdown once reaches 0" }

                if (activePlayerDeque.sum() == 0) {
                    log.info { "Shutdown score breached threshold" }
                    cosmosDaemon.shutdown(false)
                }
            }
        }
    }

    suspend fun startUpdateDatabaseStatusEveryMinute(scope: CoroutineScope, serverUUID: UUID) {
        scope.launch {
            while (true) {
                dynamoStorage.updateServerEntryNonNulls(
                    ServerEntry(
                        serverId = serverUUID.toString(),
                        lastKnownAliveTime = System.currentTimeMillis())
                )

                delay(60000)
            }
        }
    }
}

@AssistedFactory
interface TaskSchedulerFactory {
    fun create(
        gameServerLifecycleManager: GameServerLifecycleManager,
        cosmosDaemon: CosmosDaemon
    ): TaskScheduler
}
