package com.lilaceclipse.cosmos.util

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.*
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class ProcessIOAdapter(private val process: Process) {
    private val processIOExecutorService: ExecutorService = Executors.newSingleThreadExecutor()

    private val processWriter = BufferedWriter(OutputStreamWriter(process.outputStream, Charset.defaultCharset()))
    private val outputStreamReader = ParsingStreamConsumer(process.inputStream, false)
    private val errorStreamReader = ParsingStreamConsumer(process.errorStream, true)

    init {
        processIOExecutorService.submit(outputStreamReader)
        processIOExecutorService.submit(errorStreamReader)
    }

    fun writeLine(line: String) {
        processWriter.appendLine(line)
        processWriter.flush()
    }

    fun writeLineAndGetResponse(line: String, responseRegex: Regex): MatchResult? {
        if (!process.isAlive) {
            return null
        }

        writeLine(line)
        return try {
            outputStreamReader
                .matchInput(responseRegex)
                .get(10000L, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            null
        }

    }

    fun registerRegexWithCallback(regex: Regex, callback: Consumer<MatchResult>) {
        outputStreamReader.registerCallbackRegex(CallbackRegex(regex, callback))
    }

    /**
     * Must be shutdown, otherwise the program can hang
     */
    fun shutdown() {
        processIOExecutorService.shutdown()
    }

    /**
     * Stream consumer that allows for submitting expected regex's with callbacks
     */
    private class ParsingStreamConsumer(
        private val inputStream: InputStream,
        private val isErrorStream: Boolean
    ): Runnable {

        private val log = KotlinLogging.logger {}
        private val activeRegexMatchers: MutableSet<MultithreadedRegexStuff> = mutableSetOf()

        override fun run() {
            BufferedReader(InputStreamReader(inputStream, Charset.defaultCharset())).lines()
                .forEach {
                    val removeSet: MutableSet<MultithreadedRegexStuff> = mutableSetOf()
                    activeRegexMatchers.forEach { inner ->
                        if (inner.regex.matches(it)) {
                            inner.execute(inner.regex.find(it)!!)
                            removeSet.add(inner)
                        }
                    }
                    activeRegexMatchers.removeAll(removeSet)
                    printInput(it)
                }
        }

        private fun printInput(input: String) {
            if (isErrorStream) {
                log.error { input }
            } else {
                log.info { input }
            }
        }

        fun matchInput(regex: Regex): FutureRegex {
            val future = FutureRegex(regex)
            activeRegexMatchers.add(future)
            return future
        }

        fun registerCallbackRegex(callbackRegex: CallbackRegex) {
            activeRegexMatchers.add(callbackRegex)
        }
    }

    interface MultithreadedRegexStuff {
        val regex: Regex

        fun execute(matchResult: MatchResult)
    }


    /**
     * DO NOT USE run()
     */
    private class FutureRegex(override val regex: Regex): FutureTask<MatchResult>({null}), MultithreadedRegexStuff {
        private val log = KotlinLogging.logger {}

        override fun execute(matchResult: MatchResult) {
            this.set(matchResult)
        }
    }

    private class CallbackRegex(override val regex: Regex, val callback: Consumer<MatchResult>):
        MultithreadedRegexStuff {
        private val log = KotlinLogging.logger {}
        override fun execute(matchResult: MatchResult) {
            callback.accept(matchResult)
        }

    }
}