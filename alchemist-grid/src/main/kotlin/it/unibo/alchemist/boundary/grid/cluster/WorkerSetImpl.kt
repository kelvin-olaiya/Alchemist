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
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.ServerQueues.JOBS_QUEUE_METADATA_KEY
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.proto.SimulationOuterClass.RunSimulation
import java.util.UUID

class WorkerSetImpl(
    override val servers: Collection<RemoteServer>,
    private val clusterManager: ClusterInfoManagerClientFacade,
    private val dispatchStrategy: DispatchStrategy,
) : WorkerSet {

    override fun dispatchBatch(batch: SimulationBatch): Collection<UUID> {
        val jobIDs = clusterManager.submitSimulationBatch(batch)
        val assignements = dispatchStrategy.makeAssignments(servers.toList(), jobIDs)
        assignements.entries.forEach {
            it.key.metadata[JOBS_QUEUE_METADATA_KEY]?.let { jobQueue ->
                it.value.forEach { jobID ->
                    val message = RunSimulation.newBuilder().setJobID(jobID.toString()).build()
                    publishToQueue(jobQueue, message.toByteArray())
                }
            }
        }
        return jobIDs
    }
}
