package com.lilaceclipse.cosmos.gameserver

import java.util.*

interface GameServerWrapper {

    fun startServer(launchCommand: String, serverUUID: UUID)
    fun shutdownServer(force: Boolean)
    fun serverStatus(): ServerStatus
    fun executeOp()
    fun executeList(): Int?
}
