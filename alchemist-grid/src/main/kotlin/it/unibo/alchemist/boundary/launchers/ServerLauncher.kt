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
import it.unibo.alchemist.boundary.grid.cluster.management.ObservableClusterRegistry
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.boundary.grid.communication.RabbitmqConfig
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider.getEtcdEndpoints
import it.unibo.alchemist.boundary.launchers.ConfigurationProvider.getRabbitmqConfig
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Launches a service waiting for simulations to be sent over the network.
 */
class ServerLauncher(
    private val configurationPath: String,
) : SimulationLauncher() {

    private val logger = LoggerFactory.getLogger(ServerLauncher::class.java)

    override fun launch(loader: Loader) {
        RabbitmqConfig.setUpConnection(getRabbitmqConfig(configurationPath))
        val serverID = UUID.randomUUID()
        logger.debug("Server assigned ID: {}", serverID)
        val metadata = mapOf<String, String>()
        val server = AlchemistServer(
            serverID,
            ObservableClusterRegistry(EtcdKVStore(getEtcdEndpoints(configurationPath))),
        )
        logger.debug("Registering to cluster")
        server.register(metadata)
        logger.debug("Registered to cluster")
    }
}
