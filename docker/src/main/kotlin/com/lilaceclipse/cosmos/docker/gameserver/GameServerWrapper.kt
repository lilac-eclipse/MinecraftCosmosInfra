package com.lilaceclipse.cosmos.docker.gameserver

import java.util.*

interface GameServerWrapper {

    fun startServer(launchCommand: String, serverUUID: UUID)
    fun shutdownServer(force: Boolean)
    fun serverStatus(): com.lilaceclipse.cosmos.docker.gameserver.ServerStatus
    fun executeOp()
    fun executeList(): Int?
}
