package com.lilaceclipse.cosmos.docker

import com.lilaceclipse.cosmos.docker.daemon.CosmosCli
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(
        CommandLine(CosmosCli())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(*args))
}
