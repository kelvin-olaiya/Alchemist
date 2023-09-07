/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.AlchemistServer
import it.unibo.alchemist.boundary.grid.cluster.storage.KVStore
import it.unibo.alchemist.boundary.grid.simulation.Complexity
import it.unibo.alchemist.proto.ClusterMessages
import java.util.UUID

class ClusterImpl(
    private val storage: KVStore,
) : Cluster {
    override val nodes: Collection<ClusterNode> get() = storage
        .get(AlchemistServer.SERVERS_KEY)
        .map { ClusterMessages.Registration.parseFrom(it.bytes) }
        .map { AlchemistClusterNode(UUID.fromString(it.serverID), java.util.Map.copyOf(it.metadataMap)) }
        .toList()

    override fun workerSet(simulationComplexity: Complexity): Dispatcher =
        BatchDispatcher(nodes, DispatchStrategyFactory.roundRobin, storage)
}
