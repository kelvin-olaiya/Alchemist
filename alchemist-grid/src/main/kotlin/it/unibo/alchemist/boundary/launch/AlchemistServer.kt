/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */
package it.unibo.alchemist.boundary.launch

import io.etcd.jetcd.Client
import it.unibo.alchemist.boundary.Loader
import it.unibo.alchemist.boundary.launchers.SimulationLauncher
import java.util.concurrent.CountDownLatch

/**
 * Launches a service waiting for simulations to be sent over the network.
 */
object AlchemistServer : SimulationLauncher() {
    override fun launch(loader: Loader) {
        // TODO: retrieve endpoints from configuration files
        val client = Client.builder().endpoints(
            "http://localhost:10001",
            "http://localhost:10002",
            "http://localhost:10003",
        ).build()
        println("Connection established")

        client.kvClient
        CountDownLatch(1).await()
    }
}
