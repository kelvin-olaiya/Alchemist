/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.model.Time
import org.kaikikm.threadresloader.ResourceLoader
import java.nio.file.Files
import java.nio.file.Path

object SimulationContextFactory {

    fun newSimulationContext(loader: Loader, endStep: Double, endTime: Time) = object : SimulationContext {
        override val loader = loader
        override val endStep = endStep
        override val endTime = endTime
        override val dependencies = loader.remoteDependencies.associateWith {
            val dependencyURL = checkNotNull(ResourceLoader.getResource(it)) { "Could not find dependency file $it" }
            Files.readAllBytes(Path.of(dependencyURL.toURI()))
        }
    }
}
