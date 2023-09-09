/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch

class ClusterFaultDetector(
    private val localServerID: UUID,
    private val registry: ObservableRegistry,
    private val timeoutMillis: Long,
    private val maxReplyMisses: Int,
) : Runnable {
    private val cluster = ClusterImpl(registry)
    private val currentlyAlive = Collections.synchronizedSet(cluster.nodes.map { it.serverID }.toMutableSet())
    private val logger = LoggerFactory.getLogger(ClusterFaultDetector::class.java)

    override fun run() {
        currentlyAlive.filter { it != localServerID }.forEach { possiblyStartServerFaultDetector(it) }
        registry.addServerJoinListener {
            logger.debug("Fault detector watch --- {}", it.serverID)
            currentlyAlive.add(it.serverID)
            possiblyStartServerFaultDetector(it.serverID)
        }
        CountDownLatch(1).await()
    }

    private fun possiblyStartServerFaultDetector(serverID: UUID) {
        if (serverID == localServerID) {
            return
        }
        Thread(
            ServerFaultDetector(serverID, timeoutMillis, maxReplyMisses) {
                currentlyAlive.remove(it)
                logger.debug("Server {} died", it)
                registry.removeServer(it)
            },
            "Server-$serverID-fault-detector",
        ).start()
    }
}
