/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.cluster.management.Registry
import it.unibo.alchemist.boundary.grid.simulation.Complexity
import java.util.UUID

class ClusterImpl(
    private val registry: Registry,
) : Cluster {

    override val nodes: Collection<ClusterNode> get() = registry.nodes

    private val onClusterJoinCallbacks = mutableSetOf<(List<UUID>, List<UUID>) -> Unit>()

    override fun addOnClusterJoinCallback(callback: (newServers: List<UUID>, oldServers: List<UUID>) -> Unit) {
        onClusterJoinCallbacks.add(callback)
    }

    override fun removeOnClusterJoinCallback(callback: (List<UUID>, List<UUID>) -> Unit) {
        onClusterJoinCallbacks.remove(callback)
    }

    override fun workerSet(simulationComplexity: Complexity): Dispatcher =
        BatchDispatcher(nodes, DispatchStrategyFactory.roundRobin, registry)
}
