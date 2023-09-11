/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.cluster.management.ClusterFaultDetector
import it.unibo.alchemist.boundary.grid.cluster.management.ObservableRegistry
import it.unibo.alchemist.boundary.grid.cluster.management.StopFlag
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.deleteQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.grid.simulation.BatchResult
import it.unibo.alchemist.boundary.grid.simulation.BatchResultImpl
import it.unibo.alchemist.boundary.grid.simulation.SimulationBatch
import it.unibo.alchemist.boundary.grid.simulation.SimulationResult
import it.unibo.alchemist.boundary.grid.simulation.SimulationResultImpl
import it.unibo.alchemist.proto.SimulationMessage
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch

class BatchDispatcher(
    override val nodes: Collection<ClusterNode>,
    private val dispatchStrategy: DispatchStrategy,
    private val registry: ObservableRegistry,
) : Dispatcher {

    private val unreachebleNodes = Collections.synchronizedSet(mutableSetOf<ClusterNode>())
    private val reachableNodes get() = nodes.filter { it !in unreachebleNodes }
    private val stopFlag = StopFlag()

    override fun dispatchBatch(batch: SimulationBatch): BatchResult {
        val eventsQueue = declareQueue()
        val latch = CountDownLatch(batch.initializers.size)
        val results = mutableListOf<SimulationResult>()
        registerEventsHandler(eventsQueue, results, latch)
        val (simulationID, jobIDs) = registry.submitBatch(batch)
        startClusterFaultDetector(simulationID, eventsQueue)
        makeAssignmentsAndNotify(reachableNodes, jobIDs.keys.toList(), eventsQueue, registry::assignJob)
        latch.await()
        stopFlag.set()
        deleteQueue(eventsQueue)
        return BatchResultImpl(simulationID, results, registry)
    }

    private fun startClusterFaultDetector(simulationID: UUID, eventsQueue: String) {
        Thread(
            ClusterFaultDetector(registry, DEFAULT_TIMEOUT_MILLIS, DEFAULT_MAX_REPLY_MISSSES, stopFlag, null) {
                onNodeFailure(it, simulationID, eventsQueue)
            },
        ).start()
    }

    private fun onNodeFailure(serverID: UUID, simulationID: UUID, eventsQueue: String) {
        nodes.find { it.serverID == serverID }?.let { node ->
            unreachebleNodes.add(node)
            logger.debug("Server {} failed", node.serverID)
            val assignedJobs = registry.assignedJobs(node.serverID, simulationID)
            makeAssignmentsAndNotify(reachableNodes, assignedJobs.toList(), eventsQueue, registry::reassignJob)
            logger.debug("Jobs have been redistributed")
        }
    }

    private fun makeAssignmentsAndNotify(
        nodes: List<ClusterNode>,
        jobs: List<UUID>,
        replyTo: String,
        afterNotify: (jobID: UUID, serverID: UUID) -> Unit,
    ) {
        val assignements = dispatchStrategy.makeAssignments(nodes.toList(), jobs)
        assignements.entries.forEach {
            val jobQueue = CommunicationQueues.JOBS.of(it.key.serverID)
            it.value.forEach { jobID ->
                val message = SimulationMessage.JobCommand.newBuilder()
                    .setJobID(jobID.toString())
                    .setCommand(SimulationMessage.JobCommandType.RUN)
                    .build()
                publishToQueue(jobQueue, replyTo, message.toByteArray())
                afterNotify(jobID, it.key.serverID)
            }
        }
    }

    private fun registerEventsHandler(
        eventsQueue: String,
        results: MutableList<SimulationResult>,
        latch: CountDownLatch,
    ) {
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
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BatchDispatcher::class.java)
        private const val DEFAULT_TIMEOUT_MILLIS = 3000L
        private const val DEFAULT_MAX_REPLY_MISSSES = 3
    }
}
