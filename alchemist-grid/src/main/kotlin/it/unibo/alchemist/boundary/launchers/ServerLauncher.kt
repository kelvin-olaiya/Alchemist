/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.launchers

import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.grid.AlchemistServer
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Launches a service waiting for simulations to be sent over the network.
 */
class ServerLauncher : SimulationLauncher() {

    private val logger = LoggerFactory.getLogger(ServerLauncher::class.java)

    override fun launch(loader: Loader) {
        val endpoints = listOf("http://localhost:10001", "http://localhost:10002", "http://localhost:10003")
        val serverID = UUID.randomUUID()
        /*val healthCheckResponseMessage = HealthCheckResponse.newBuilder()
            .setServerID(serverID.toString())
            .build()
            .toByteArray()*/
        logger.debug("Server assigned ID: {}", serverID)
        val metadata = mapOf<String, String>()
        val server = AlchemistServer(serverID, EtcdKVStore(endpoints))
        logger.debug("Registering to cluster")
        server.register(metadata)
        logger.debug("Registered to cluster")

        /*channel.queueDeclare(healthQueue, false, false, false, null)
        registerQueueConsumer(healthQueue) { _, delivery ->
            logger.debug("{} - Received helth check request", serverID)
            val replyTo = delivery.properties.replyTo
            publishToQueue(replyTo, healthCheckResponseMessage)
            logger.debug("Sent health check response to {} queue", replyTo)
        }*/

        /*channel.queueDeclare(jobsQueue, false, false, false, null)
        registerQueueConsumer(jobsQueue) { _, delivery ->
            val jobId = RunSimulation.parseFrom(delivery.body).jobID
            logger.debug("Received job order $jobId")
            val simulation = clusterManager.getSimulation<Any, _>(UUID.fromString(jobId))
            println(simulation.toString())
        }
        logger.debug("health-queue registered $healthQueue")
        Thread(ClusterHealthChecker(clusterManager, 500, 1)).start()
        logger.debug("health-checker started")*/
    }
}
