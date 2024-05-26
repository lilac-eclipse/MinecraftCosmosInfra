package com.lilaceclipse.cosmos.docker.daemon

import com.lilaceclipse.cosmos.gameserver.GameServerFactory
import com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper
import com.lilaceclipse.cosmos.storage.DynamoStorage
import com.lilaceclipse.cosmos.storage.OnlineStatus
import com.lilaceclipse.cosmos.storage.S3Storage
import com.lilaceclipse.cosmos.storage.ServerEntry
import com.lilaceclipse.cosmos.util.EcsUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Paths
import java.util.*

class GameServerLifecycleManager @AssistedInject constructor(
    private val gameServerFactory: GameServerFactory,
    private val s3Storage: S3Storage,
    private val dynamoStorage: DynamoStorage,
    private val ecsUtil: EcsUtil,
    @Assisted private val serverUUID: UUID
) {
    private val GAME_STORAGE_DIR = Paths.get("/opt/gameserver/")
    private val log = KotlinLogging.logger {}

    var gameServerWrapper: com.lilaceclipse.cosmos.docker.gameserver.GameServerWrapper? = null
    private var s3KeySuffix: String? = null

    fun runServer() {
        log.info { "Starting game server!" }

        val containerIp = ecsUtil.getContainerPublicIp()
        log.info { "ip: $containerIp" }
        dynamoStorage.updateServerEntryNonNulls(ServerEntry(
            serverId = serverUUID.toString(),
            onlineStatus = OnlineStatus.SERVER_STARTING,
            ipAddress = containerIp))
        log.info { "made call!" }
        val serverEntry = dynamoStorage.getServerEntryFromDb(serverUUID.toString())
        log.info { serverEntry }
        s3KeySuffix = serverEntry.s3KeySuffix

        s3Storage.downloadMinecraft(GAME_STORAGE_DIR, s3KeySuffix!!)

        // TODO set gameServerWrapper to something before download is complete, or update how status checking
        //  works in route handler/task scheduler, otherwise there's a possibility that they detect null
        //  and abort early
        gameServerWrapper = gameServerFactory.create(GAME_STORAGE_DIR)
        gameServerWrapper!!.startServer(serverEntry.launchCommand!!, serverUUID)

        // TODO block until server shuts down/implement auto restart
    }

    fun stopServer(force: Boolean) {
        log.info { "Stopping game server!" }

        dynamoStorage.updateServerEntryNonNulls(ServerEntry(
            serverId = serverUUID.toString(),
            onlineStatus = OnlineStatus.SERVER_STOPPING))

        if (!force) {
            gameServerWrapper?.shutdownServer(false)
            log.info { "Server has been shutdown" }

        } else {
            gameServerWrapper?.shutdownServer(true)
            log.info { "Server has been forcibly shutdown" }
        }

        log.info { "Starting game server upload" }
        s3Storage.uploadMinecraft(GAME_STORAGE_DIR, s3KeySuffix!!)

        dynamoStorage.updateServerEntryNonNulls(ServerEntry(
            serverId = serverUUID.toString(),
            onlineStatus = OnlineStatus.OFFLINE,
            ipAddress = ""))
    }
}

@AssistedFactory
interface GameServerLifecycleManagerFactory {
    fun create(serverUUID: UUID): GameServerLifecycleManager
}