/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatchImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationConfig
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import java.util.UUID

interface WorkerSet {

    /**
     * The remote servers in this [WorkerSet].
     */
    val servers: Collection<RemoteServer>

    /**
     * Dispatch the [SimulationInitializer]s for the
     * provided [SimulationConfig].
     */
    fun dispatchBatch(
        simulationConfig: SimulationConfig,
        simulationInitializers: Collection<SimulationInitializer>,
    ): Collection<UUID> = dispatchBatch(SimulationBatchImpl(simulationConfig, simulationInitializers))

    /**
     * Dispatch the [SimulationBatch].
     */
    fun dispatchBatch(batch: SimulationBatch): Collection<UUID>
}
