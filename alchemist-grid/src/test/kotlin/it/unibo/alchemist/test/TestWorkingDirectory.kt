/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import it.unibo.alchemist.boundary.LoadAlchemist
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfigImpl
import it.unibo.alchemist.boundary.grid.utils.WorkingDirectory
import it.unibo.alchemist.model.Time
import org.kaikikm.threadresloader.ResourceLoader
import java.io.File
import java.net.URL

class TestWorkingDirectory : StringSpec({
    "Files should be correctly written in working directory" {
        val resource = "config/00-dependencies.yml"
        val yaml = ResourceLoader.getResource(resource)
        withClue("Yaml configuration should exits") {
            yaml.shouldNotBeNull()
        }
        val loader: Loader = getLoader(yaml)
        val simulationConfiguration = SimulationConfigImpl(loader, 0, Time.INFINITY)
        simulationConfiguration.dependencies shouldHaveSize 2
        val test: File
        WorkingDirectory().use { wd ->
            test = File(wd.getFileAbsolutePath("nothing")).parentFile
            test.exists().shouldBeTrue()
            wd.writeFiles(simulationConfiguration.dependencies)
            val newFile = File(wd.getFileAbsolutePath("test.txt"))
            if (newFile.exists() || newFile.createNewFile()) {
                ResourceLoader.addURL(wd.url)
                ResourceLoader.getResource("test.txt").shouldNotBeNull()
            } else {
                error("File was not written in working directory")
            }
        }
        test.exists().shouldBeFalse()
    }
}) {
    companion object {
        private fun getLoader(yaml: URL) = LoadAlchemist.from(yaml)
    }
}
