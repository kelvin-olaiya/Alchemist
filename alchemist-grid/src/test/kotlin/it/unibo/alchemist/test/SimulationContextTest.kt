/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import it.unibo.alchemist.test.utils.GridTestUtils.getSimulationContext
import org.kaikikm.threadresloader.ResourceLoader
import java.nio.file.Files
import java.nio.file.Path

class SimulationContextTest : StringSpec({
    "Can create a simulation context" {
        getSimulationContext(YAML_CONFIG_PATH)
    }

    "Simulation dependencies are correctly loaded" {
        val simulationContext = getSimulationContext(YAML_CONFIG_PATH)
        simulationContext.dependencies.size shouldBeExactly 2
        simulationContext.dependencies[DEPENDENCY_FILE_PATH] shouldBe Files.readAllBytes(
            Path.of(ResourceLoader.getResource(DEPENDENCY_FILE_PATH).toURI()),
        )
    }
}) {
    companion object {

        private const val YAML_CONFIG_PATH = "config/00-dependencies.yml"
        private const val DEPENDENCY_FILE_PATH = "config/dependencies_test.txt"
    }
}
