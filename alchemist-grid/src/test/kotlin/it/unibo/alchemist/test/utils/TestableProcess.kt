/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test.utils

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.stream.Collector
import java.util.stream.Collectors

class TestableProcess(
    private val process: Process,
    private val stdout: File,
    private val stderr: File,
) : AutoCloseable {

    private fun <R> readAll(file: File, f: Collector<in String, *, R>): R {
        try {
            BufferedReader(FileReader(file)).use { reader ->
                return reader.lines().collect(f)
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun stdin(): BufferedWriter {
        return BufferedWriter(OutputStreamWriter(process.outputStream))
    }

    fun stdoutAsText(): String {
        return readAll(stdout, Collectors.joining("\n"))
    }

    fun stderrAsText(): String {
        return readAll(stderr, Collectors.joining("\n"))
    }

    fun stdoutAsLines(): List<String> {
        return readAll(stdout, Collectors.toList())
    }

    fun stderrAsLines(): List<String> {
        return readAll(stderr, Collectors.toList())
    }

    override fun close() {
        if (process.isAlive) {
            process.destroyForcibly()
        }
        if (stdout.exists()) {
            stdout.delete()
        }
        if (stderr.exists()) {
            stderr.delete()
        }
    }

    fun printDebugInfo(processName: String) {
        System.out.printf("Stdout of `%s`:\n> ", process.info().commandLine().orElse(processName))
        println(stdoutAsText().replace("\n", "\n> "))
        print("stderr of the same process:\n> ")
        println(stderrAsText().replace("\n", "\n> "))
        println()
    }

    @Throws(IOException::class)
    fun feedStdin(line: String) {
        stdin().write(line)
        stdin().write("\n")
        stdin().flush()
    }

    fun awaitOutputContains(line: String) {
        while (!outputContains(line)) {
            // busy wait
        }
    }

    fun outputContains(line: String) = stdout.exists() && stdoutAsLines().stream().anyMatch { it.contains(line) }

    fun waitFor() = process.waitFor()
}
