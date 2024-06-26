package com.lilaceclipse.cosmos.docker.gameserver

import com.lilaceclipse.cosmos.docker.storage.DynamoStorage
import com.lilaceclipse.cosmos.docker.storage.S3Storage
import java.nio.file.Path
import javax.inject.Inject

class GameServerFactory @Inject constructor(
    private val dynamoStorage: DynamoStorage
) {

    fun create(mcStorageDir: Path) : MinecraftServerWrapper {
        return MinecraftServerWrapper(mcStorageDir, dynamoStorage)
    }
}