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
import it.unibo.alchemist.boundary.grid.cluster.RabbitmqConfig.HEALTH_QUEUE_METADATA_KEY
import it.unibo.alchemist.boundary.grid.cluster.RabbitmqConfig.JOBS_QUEUE_METADATA_KEY
import it.unibo.alchemist.boundary.grid.cluster.RabbitmqConfig.channel
import it.unibo.alchemist.boundary.grid.cluster.RabbitmqConfig.getQueueNameFor
import it.unibo.alchemist.boundary.grid.cluster.management.ClusterHealthChecker
import it.unibo.alchemist.boundary.grid.cluster.management.ClusterManagerImpl
import it.unibo.alchemist.boundary.grid.cluster.management.ServerMetadata
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.proto.Cluster.HealthCheckResponse
import it.unibo.alchemist.proto.Common
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Launches a service waiting for simulations to be sent over the network.
 */
class AlchemistServer : SimulationLauncher() {
    private val logger = LoggerFactory.getLogger(AlchemistServer::class.java)

    override fun launch(loader: Loader) {
        val endpoints = listOf("http://localhost:10001", "http://localhost:10002", "http://localhost:10003")
        val serverID = UUID.randomUUID()
        logger.debug("Server assigned ID: {}", serverID)
        val healthQueue = getQueueNameFor(serverID, "health")
        val jobsQueue = getQueueNameFor(serverID, "jobs")
        val metadata = mapOf(
            HEALTH_QUEUE_METADATA_KEY to healthQueue,
            JOBS_QUEUE_METADATA_KEY to jobsQueue,
        )
        val clusterManager = ClusterManagerImpl(EtcdKVStore(endpoints))
        logger.debug("Registering to cluster")
        clusterManager.join(serverID, ServerMetadata(metadata))
        logger.debug("Registered to cluster")
        channel.queueDeclare(healthQueue, false, false, false, null)
        channel.basicConsume(healthQueue, false, { _, delivery ->
            val replyTo = delivery.properties.replyTo
            channel.basicPublish(
                "",
                replyTo,
                null,
                HealthCheckResponse.newBuilder()
                    .setServerID(Common.ID.newBuilder().setValue(serverID.toString()))
                    .build()
                    .toByteArray(),
            )
            println("$serverID - Received helth check request")
        }, { _ -> })
        logger.debug("health-queue registered $healthQueue")
        Thread(ClusterHealthChecker(clusterManager, 500, 1)).start()
        logger.debug("health-checker started")
    }
}
