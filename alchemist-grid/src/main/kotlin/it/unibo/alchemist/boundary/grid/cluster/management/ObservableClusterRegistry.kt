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
import it.unibo.alchemist.boundary.grid.cluster.AlchemistClusterNode
import it.unibo.alchemist.boundary.grid.cluster.ClusterNode
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.proto.ClusterMessages.Registration
import org.slf4j.LoggerFactory
import java.util.UUID

class ObservableClusterRegistry(
    private val storage: KVStore,
) : Registry by ClusterRegistry(storage), ObservableRegistry {

    override fun addServerJoinListener(listener: (ClusterNode) -> Unit) {
        storage.watchPut(ClusterRegistry.Companion.KEYS.SERVERS.prefix) { new, _ ->
            val registration = Registration.parseFrom(new.toByteArray())
            val clusterNode = AlchemistClusterNode(UUID.fromString(registration.serverID), registration.metadataMap)
            listener(clusterNode)
        }
    }

    override fun addServerLeaveListener(listener: (UUID) -> Unit) {
        storage.watchDelete(ClusterRegistry.Companion.KEYS.SERVERS.prefix) { new, old ->
            val newServer = Registration.parseFrom(new.bytes)
            val oldServer = Registration.parseFrom(old.bytes)
            logger.debug("NEW ${newServer.serverID}")
            logger.debug("OLD ${oldServer.serverID}")
        }
    }

    private fun ByteSequence.toByteArray() = this.bytes

    companion object {
        private val logger = LoggerFactory.getLogger(ObservableClusterRegistry::class.java)
    }
}
