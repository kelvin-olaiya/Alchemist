/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.simulation

import it.unibo.alchemist.boundary.grid.cluster.management.Registry
import java.util.UUID

class BatchResultImpl(
    private val simulationID: UUID,
    override val results: Collection<SimulationResult>,
    private val registry: Registry,
) : BatchResult {

    override val numOfErrors: Int get() = results.count { it.error.isPresent }

    override fun saveAllLocaly(exportPath: String) {
        results.forEach { it.saveLocally(exportPath) }
        registry.deleteSimulation(simulationID)
        registry.clearResults(simulationID)
    }
}
