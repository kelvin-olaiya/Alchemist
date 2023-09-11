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
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.grid.simulation.BatchResult
import it.unibo.alchemist.boundary.grid.simulation.BatchResultImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationResult
import it.unibo.alchemist.boundary.grid.simulation.SimulationResultImpl
import it.unibo.alchemist.proto.SimulationMessage
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.CountDownLatch

class BatchDispatcher(
    override val nodes: Collection<ClusterNode>,
    private val dispatchStrategy: DispatchStrategy,
    private val registry: Registry,
) : Dispatcher {

    override fun dispatchBatch(batch: SimulationBatch): BatchResult {
        val eventsQueue = channel.queueDeclare().queue
        val latch = CountDownLatch(batch.initializers.size)
        val results = mutableListOf<SimulationResult>()
        registerQueueConsumer(eventsQueue) { _, delivery ->
            val event = SimulationMessage.SimulationEvent.parseFrom(delivery.body)
            when (event.type) {
                SimulationMessage.SimulationEventType.RECEIVED -> {
                    logger.debug("Job {} has been received by {}", event.jobID, event.serverID)
                }
                SimulationMessage.SimulationEventType.STARTED -> {
                    logger.debug("Server {} has started job {}", event.serverID, event.jobID)
                }
                SimulationMessage.SimulationEventType.COMPLETED -> {
                    logger.debug("Server {} has completed job {}", event.serverID, event.jobID)
                    results.add(SimulationResultImpl(UUID.fromString(event.jobID), registry))
                    latch.countDown()
                    logger.debug("Remains {}", latch.count)
                }
                SimulationMessage.SimulationEventType.CANCELLED -> {
                    logger.debug("Job {} has been cancelled", event.jobID)
                    latch.countDown()
                }
                SimulationMessage.SimulationEventType.ERROR -> {
                    logger.debug("Job {} has encountered an error", event.jobID)
                    results.add(SimulationResultImpl(UUID.fromString(event.jobID), registry))
                    latch.countDown()
                }
                SimulationMessage.SimulationEventType.UNRECOGNIZED -> {
                    logger.debug("An unknow event has been received for job {}", event.jobID)
                }
                null -> {}
            }
        }
        val (simulationID, jobIDs) = registry.submitBatch(batch)
        val assignements = dispatchStrategy.makeAssignments(nodes.toList(), jobIDs.keys.toList())
        assignements.entries.forEach {
            val jobQueue = CommunicationQueues.JOBS.of(it.key.serverID)
            it.value.forEach { jobID ->
                val message = SimulationMessage.JobCommand.newBuilder()
                    .setJobID(jobID.toString())
                    .setCommand(SimulationMessage.JobCommandType.RUN)
                    .build()
                publishToQueue(jobQueue, eventsQueue, message.toByteArray())
                registry.assignJob(jobID, it.key.serverID)
            }
        }
        latch.await()
        channel.queueDelete(eventsQueue)
        return BatchResultImpl(simulationID, results, registry)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchDispatcher::class.java)
    }
}
