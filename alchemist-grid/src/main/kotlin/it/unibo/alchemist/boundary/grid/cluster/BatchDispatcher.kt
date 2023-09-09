/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.cluster.management.Registry
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig.channel
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import it.unibo.alchemist.proto.SimulationMessage
import java.util.UUID

class BatchDispatcher(
    override val nodes: Collection<ClusterNode>,
    private val dispatchStrategy: DispatchStrategy,
    private val registry: Registry,
) : Dispatcher {

    override fun dispatchBatch(batch: SimulationBatch): Map<UUID, SimulationInitializer> {
        val eventsQueue = channel.queueDeclare().queue
        val (_, jobIDs) = registry.submitBatch(batch)
        val assignements = dispatchStrategy.makeAssignments(nodes.toList(), jobIDs.keys.toList())
        assignements.entries.forEach {
            val jobQueue = CommunicationQueues.JOBS.of(it.key.serverID)
            it.value.forEach { jobID ->
                val message = SimulationMessage.RunSimulation.newBuilder().setJobID(jobID.toString()).build()
                publishToQueue(jobQueue, eventsQueue, message.toByteArray())
            }
        }
        /*registerQueueConsumer(eventsQueue) { _, delivery ->
            val event = SimulationEvent.parseFrom(delivery.body)
        }*/
        return jobIDs
    }
}
