package com.lilaceclipse.cosmos.docker.storage

enum class OnlineStatus {
    CONTAINER_LAUNCHED,
    SERVER_STARTING,
    RUNNING,
    SERVER_STOPPING,
    OFFLINE
}