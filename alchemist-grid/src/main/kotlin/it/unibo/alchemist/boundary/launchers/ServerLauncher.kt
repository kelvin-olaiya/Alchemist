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
import it.unibo.alchemist.boundary.grid.cluster.management.ClusterRegistry
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
        logger.debug("Server assigned ID: {}", serverID)
        val metadata = mapOf<String, String>()
        val server = AlchemistServer(serverID, ClusterRegistry(EtcdKVStore(endpoints)))
        logger.debug("Registering to cluster")
        server.register(metadata)
        logger.debug("Registered to cluster")
    }
}
