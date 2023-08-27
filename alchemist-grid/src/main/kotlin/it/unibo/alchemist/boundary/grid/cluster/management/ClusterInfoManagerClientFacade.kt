/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.simulation.SimulationConfig
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import java.util.UUID

interface ClusterInfoManagerClientFacade : ClusterInfoManager {

    /**
     * Submit the simulation configuration to the cluster storage.
     */
    fun submitSimulationConfiguration(configuration: SimulationConfig): UUID

    /**
     * Submits the simulation initializer to the cluster storage.
     */
    fun submitSimulationInitializers(
        simulationID: UUID,
        initializers: Collection<SimulationInitializer>,
    ): Collection<UUID>
}
