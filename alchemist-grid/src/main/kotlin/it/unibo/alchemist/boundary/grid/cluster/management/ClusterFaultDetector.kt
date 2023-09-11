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

class ClusterFaultDetector(
    private val registry: ObservableRegistry,
    private val timeoutMillis: Long,
    private val maxReplyMisses: Int,
    private val stopFlag: StopFlag,
    private val localServerID: UUID? = null,
    private val onNodeFault: (UUID) -> Unit = {},
) : Runnable {
    private val cluster = ClusterImpl(registry)
    private val currentlyAlive = Collections.synchronizedSet(cluster.nodes.map { it.serverID }.toMutableSet())
    private val logger = LoggerFactory.getLogger(ClusterFaultDetector::class.java)

    override fun run() {
        currentlyAlive.filter { it != localServerID }.forEach { possiblyStartServerFaultDetector(it) }
        registry.addServerJoinListener {
            currentlyAlive.add(it.serverID)
            possiblyStartServerFaultDetector(it.serverID)
        }
        while (!stopFlag.isSet()) {
            Thread.sleep(CHECK_INTERVAL)
        }
    }

    private fun possiblyStartServerFaultDetector(serverID: UUID) {
        if (serverID == localServerID) {
            return
        }
        Thread(
            ServerFaultDetector(serverID, timeoutMillis, maxReplyMisses, stopFlag) {
                currentlyAlive.remove(it)
                logger.debug("Server {} died", it)
                registry.removeServer(it)
                onNodeFault(it)
            },
            "Server-$serverID-fault-detector",
        ).start()
        logger.debug("Fault detector watch will watch {}", serverID)
    }

    companion object {
        private const val CHECK_INTERVAL = 3000L
    }
}
