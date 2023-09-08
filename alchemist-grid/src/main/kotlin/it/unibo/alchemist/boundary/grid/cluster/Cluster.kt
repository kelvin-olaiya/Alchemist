/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import it.unibo.alchemist.boundary.grid.simulation.Complexity
import java.util.UUID

interface Cluster {

    /**
     * A set of remote servers for the given [simulationComplexity].
     */
    fun workerSet(simulationComplexity: Complexity): Dispatcher

    /**
     * The remote servers currently joining the cluster.
     */
    val nodes: Collection<ClusterNode>

    fun addOnClusterJoinCallback(callback: (newServers: List<UUID>, oldServers: List<UUID>) -> Unit)

    fun removeOnClusterJoinCallback(callback: (List<UUID>, List<UUID>) -> Unit)
}
