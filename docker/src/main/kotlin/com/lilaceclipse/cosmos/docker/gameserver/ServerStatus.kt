package com.lilaceclipse.cosmos.docker.gameserver

enum class ServerStatus() {
    WAITING,
    STARTING,
    RUNNING,
    SHUTDOWN,
    ERROR
}
