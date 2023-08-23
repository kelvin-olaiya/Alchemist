/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.simulation.SimulationContext
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import java.util.UUID

interface WorkerSet {

    /**
     * The remote servers in this [WorkerSet].
     */
    val servers: Collection<AlchemistRemoteServer>

    /**
     * Dispatch the [SimulationInitializer]s for the
     * provided [SimulationContext].
     */
    fun dispatchJobs(
        simulationContext: SimulationContext,
        simulationInitializers: Collection<SimulationInitializer>,
    ): Collection<UUID>
}
