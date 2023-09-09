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
import it.unibo.alchemist.boundary.grid.communication.CommunicationQueues
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.declareQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.publishToQueue
import it.unibo.alchemist.boundary.grid.communication.RabbitmqUtils.registerQueueConsumer
import it.unibo.alchemist.boundary.grid.simulation.ObservableSimulation
import it.unibo.alchemist.proto.ClusterMessages.HealthCheckResponse
import it.unibo.alchemist.proto.SimulationMessage.RunSimulation
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

class AlchemistServer(
    private val serverID: UUID,
    private val registry: ObservableRegistry,
) {
    private val logger = LoggerFactory.getLogger(AlchemistServer::class.java)
    private val executor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val assignedJobs = mutableMapOf<UUID, Future<*>>()
    private val heartbeatResponse = HealthCheckResponse.newBuilder()
        .setServerID(serverID.toString())
        .build()
        .toByteArray()

    init {
        Thread(ClusterFaultDetector(serverID, registry, 1500, 3)).start()
        val jobQueue = CommunicationQueues.JOBS.of(serverID)
        declareQueue(jobQueue)
        registerQueueConsumer(jobQueue) { _, delivery ->
            val jobID = UUID.fromString(RunSimulation.parseFrom(delivery.body).jobID)
            val simulation = ObservableSimulation(registry.simulationByJobId<Any, _>(jobID), jobID)
            simulation.addCompletionCallback { println("simulation terminated") }
            val future = executor.submit(simulation)
            assignedJobs[jobID] = future
        }
        val heartbeatQueue = CommunicationQueues.HEALTH.of(serverID)
        declareQueue(heartbeatQueue)
        registerQueueConsumer(heartbeatQueue) { _, delivery ->
            logger.debug("Heartbeat request received")
            val replyTo = delivery.properties.replyTo
            publishToQueue(replyTo, heartbeatResponse)
            logger.debug("Sent heartbeat response")
        }
        logger.debug("{} registered helth queue --- {}", serverID, heartbeatQueue)
    }

    fun register(serverMetadata: Map<String, String> = mapOf()) {
        registry.addServer(serverID, serverMetadata)
    }

    fun deregister() {
        registry.removeServer(serverID)
    }

    val cluster get() = ClusterImpl(registry)

    val runningSimulations get() = (executor as ThreadPoolExecutor).activeCount
}
