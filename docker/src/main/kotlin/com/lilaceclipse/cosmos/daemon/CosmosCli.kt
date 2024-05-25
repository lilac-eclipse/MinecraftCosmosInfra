package com.lilaceclipse.cosmos.daemon

import com.lilaceclipse.cosmos.config.Environment
import com.lilaceclipse.cosmos.dagger.DaggerCosmosComponent
import com.lilaceclipse.cosmos.dagger.EnvironmentModule
import io.github.oshai.kotlinlogging.KotlinLogging
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.*
import java.util.concurrent.Callable

@Command(
    name = "cosmos",
    version = ["Cosmos CLI v1.0.0"],
    description = ["Launches the Cosmos application."],
    mixinStandardHelpOptions = true
)
class CosmosCli : Callable<Int> {

    private val log = KotlinLogging.logger {}

    @Option(
        names = ["-e", "--environment"],
        description = ["The environment to launch Cosmos in."],
        required = true,
        paramLabel = "ENV"
    )
    private lateinit var launchEnvironment: Environment

    @Option(
        names = ["-t", "--target-server"],
        description = ["The UUIDv4 of the server to launch on Cosmos."],
        required = true,
        paramLabel = "UUID"
    )
    private lateinit var targetServerUuid: UUID

    override fun call(): Int {
        log.info("Launching Cosmos daemon in $launchEnvironment environment! Server UUID: $targetServerUuid")
        val cosmosDaemon = DaggerCosmosComponent.builder()
            .environmentModule(EnvironmentModule(launchEnvironment))
            .build()
            .provideCosmosDaemon()

        cosmosDaemon.run(targetServerUuid)
        log.info { "Cosmos daemon exited" }
        return 0
    }
}
