/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test.utils

import com.palantir.docker.compose.DockerComposeExtension
import io.kotest.core.extensions.Extension
import io.kotest.core.listeners.AfterTestListener
import io.kotest.core.listeners.BeforeTestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import it.unibo.alchemist.Alchemist
import it.unibo.alchemist.boundary.LoadAlchemist
import it.unibo.alchemist.boundary.grid.simulation.SimulationContext
import it.unibo.alchemist.boundary.grid.simulation.SimulationContextFactory
import it.unibo.alchemist.model.Time
import org.kaikikm.threadresloader.ResourceLoader
import java.io.File
import java.net.URL
import kotlin.reflect.jvm.jvmName

object GridTestUtils {

    fun getLoader(yaml: URL) = LoadAlchemist.from(yaml)

    fun getSimulationContext(yamlConfigurationPath: String): SimulationContext {
        val loader = getLoader(ResourceLoader.getResource(yamlConfigurationPath))
        return SimulationContextFactory.newSimulationContext(loader, Double.MAX_VALUE, Time.INFINITY)
    }

    fun getDockerExtension(composeFilePath: String) =
        DockerComposeExtension.builder().file(composeFilePath).build().kotest

    private val DockerComposeExtension.kotest get(): Extension = object : BeforeTestListener, AfterTestListener {
        override suspend fun beforeAny(testCase: TestCase) {
            before()
        }

        override suspend fun afterAny(testCase: TestCase, result: TestResult) {
            after()
        }
    }

    fun startAlchemistProcess(vararg commandLine: String): Process {
        val javaExecutable = File(System.getProperty("java.home") + "/bin/java").absolutePath
        val classpath = System.getProperty("java.class.path")
        return ProcessBuilder(javaExecutable, "-classpath", classpath, Alchemist::class.jvmName, *commandLine)
            .redirectOutput(File("test-output.txt"))
            .start()
    }
}
