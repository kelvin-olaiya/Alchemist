/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster.management

import io.etcd.jetcd.ByteSequence
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.proto.ClusterMessages.Registration
import org.slf4j.LoggerFactory
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch

class ClusterFaultDetector(
    private val localServerID: UUID,
    private val storage: KVStore,
    private val timeoutMillis: Long,
    private val maxReplyMisses: Int,
) : Runnable {
    private val cluster = ClusterImpl(storage)
    private val currentlyAlive = Collections.synchronizedSet(cluster.nodes.map { it.serverID }.toMutableSet())
    private val logger = LoggerFactory.getLogger(ClusterFaultDetector::class.java)

    override fun run() {
        currentlyAlive.filter { it != localServerID }.forEach { possiblyStartServerFaultDetector(it) }
        storage.watchPut(SERVERS_KEY) { new, _ ->
            val newServerID = UUID.fromString(Registration.parseFrom(new.toByteArray()).serverID)
            logger.debug("Fault detector watch --- {}", newServerID)
            currentlyAlive.add(newServerID)
            possiblyStartServerFaultDetector(newServerID)
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
                storage.delete("$SERVERS_KEY/$it")
            },
            "Server-$serverID-fault-detector",
        ).start()
    }

    private fun ByteSequence.toByteArray() = this.bytes

    companion object {
        private const val SERVERS_KEY = "servers"
    }
}
