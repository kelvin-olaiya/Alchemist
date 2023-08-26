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
import it.unibo.alchemist.boundary.grid.cluster.manager.ClusterManagerImpl
import it.unibo.alchemist.boundary.grid.cluster.manager.EtcdHelper
import it.unibo.alchemist.boundary.grid.cluster.manager.ServerMetadata
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
        with(ClusterManagerImpl(EtcdHelper(endpoints))) {
            logger.debug("Registering to cluster")
            join(serverID, ServerMetadata("prova"))
            logger.debug("Registered to cluster")
        }
    }
}
