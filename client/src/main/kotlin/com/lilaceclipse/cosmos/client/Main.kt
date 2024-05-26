package com.lilaceclipse.cosmos.client

import com.lilaceclipse.cosmos.daemon.CosmosCli
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(
        CommandLine(CosmosCli())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(*args))
}
