/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.cluster.management.ClusterInfoManagerClientFacade
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import java.util.UUID

class WorkerSetImpl(
    override val servers: Collection<RemoteServer>,
    private val clusterManager: ClusterInfoManagerClientFacade,
    private val dispatchStrategy: DispatchStrategy,
) : WorkerSet {

    override fun dispatchBatch(batch: SimulationBatch): Collection<UUID> {
        val simulationID = clusterManager.submitSimulationConfiguration(batch.configuration)
        val simulationInitializersIds = clusterManager.submitSimulationInitializers(simulationID, batch.initializers)
        // val assignements = dispatchStrategy.makeAssignments(servers.toList(), batch.initializers.toList())
        // RABBIT MQ
        return simulationInitializersIds
    }
}
