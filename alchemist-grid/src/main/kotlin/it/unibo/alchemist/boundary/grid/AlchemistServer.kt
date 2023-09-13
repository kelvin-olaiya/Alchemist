/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid

import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.management.ClusterFaultDetector
import it.unibo.alchemist.boundary.grid.cluster.management.ObservableRegistry
import it.unibo.alchemist.boundary.grid.cluster.management.StopFlag
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.grid.simulation.JobStatus
import it.unibo.alchemist.boundary.grid.simulation.ObservableSimulation
import it.unibo.alchemist.proto.ClusterMessages.HealthCheckResponse
import it.unibo.alchemist.proto.SimulationMessage
import it.unibo.alchemist.proto.SimulationMessage.JobCommand
import it.unibo.alchemist.proto.SimulationMessage.SimulationEvent
import it.unibo.alchemist.proto.SimulationMessage.SimulationEventType
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class AlchemistServer(
    private val serverID: UUID,
    private val registry: ObservableRegistry,
) {
    private val heartbeatResponse = HealthCheckResponse.newBuilder()
        .setServerID(serverID.toString())
        .build()
        .toByteArray()
    private val faultDetectorStopFlag = StopFlag()
    private val executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val assignedJobs = mutableMapOf<UUID, Future<*>>()

    init {
        Thread(ClusterFaultDetector(registry, 1500, 3, faultDetectorStopFlag, serverID)).start()
        val jobQueue = CommunicationQueues.JOBS.of(serverID)
        declareQueue(jobQueue)
        registerQueueConsumer(jobQueue) { _, delivery ->
            val command = JobCommand.parseFrom(delivery.body)
            val jobID = UUID.fromString(command.jobID)
            when (command.command) {
                SimulationMessage.JobCommandType.RUN -> {
                    notifyEvent(jobID, SimulationEventType.RECEIVED, delivery.properties.replyTo)
                    runSimulation(jobID, delivery.properties.replyTo)
                    logger.debug("Received order to run job {}", jobID)
                }
                SimulationMessage.JobCommandType.CANCEL -> {
                    cancelSimulation(UUID.fromString(command.jobID), delivery.properties.replyTo)
                }
                SimulationMessage.JobCommandType.UNRECOGNIZED -> {}
                null -> {}
            }
        }
        val heartbeatQueue = CommunicationQueues.HEALTH.of(serverID)
        declareQueue(heartbeatQueue)
        registerQueueConsumer(heartbeatQueue) { _, delivery ->
            val replyTo = delivery.properties.replyTo
            publishToQueue(replyTo, heartbeatResponse)
        }
        logger.debug("Server {} registered helth queue --- {}", serverID, heartbeatQueue)
    }

    fun register(serverMetadata: Map<String, String> = mapOf()) {
        registry.addServer(serverID, serverMetadata)
    }

    fun deregister() {
        registry.removeServer(serverID)
    }

    private fun runSimulation(jobID: UUID, notifyTo: String) {
        val simulation = ObservableSimulation(registry.simulationByJobId<Any, _>(jobID), jobID)
        simulation.addStartCallback {
            registry.setJobStatus(serverID, it, JobStatus.RUNNING)
            notifyEvent(jobID, SimulationEventType.STARTED, notifyTo)
            logger.debug("Started job {}", it)
        }
        simulation.addCompletionCallback {
            registry.setJobStatus(serverID, it, JobStatus.DONE)
            notifyEvent(jobID, SimulationEventType.COMPLETED, notifyTo)
            logger.debug("Completed job {}", it)
        }
        simulation.addOnErrorCallback { id, error ->
            registry.setJobFailure(serverID, id, error)
            notifyEvent(jobID, SimulationEventType.ERROR, notifyTo)
            logger.debug("Job {} encontered an error", id)
        }
        registry.setJobStatus(serverID, jobID, JobStatus.DISPATCHED)
        val future = executor.submit(simulation)
        assignedJobs[jobID] = future
    }

    private fun cancelSimulation(jobID: UUID, notifyTo: String) {
        assignedJobs[jobID]?.cancel(true)
        notifyEvent(jobID, SimulationEventType.CANCELLED, notifyTo)
    }

    private fun notifyEvent(jobID: UUID, type: SimulationEventType, to: String) {
        publishToQueue(to, getSimulationEventMessage(jobID, type).toByteArray())
    }

    private fun getSimulationEventMessage(jobID: UUID, type: SimulationEventType): SimulationEvent =
        SimulationEvent.newBuilder()
            .setServerID(serverID.toString())
            .setJobID(jobID.toString())
            .setType(type)
            .build()

    val cluster get() = ClusterImpl(registry)

    val runningSimulations get() = (executor as ThreadPoolExecutor).activeCount

    companion object {
        private val logger = LoggerFactory.getLogger(AlchemistServer::class.java)
    }
}
