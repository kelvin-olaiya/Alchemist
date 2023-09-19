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

interface Cluster {

    /**
     * Returns [Dispatcher] suitable for the provided [simulationComplexity].
     */
    fun dispatcherFor(simulationComplexity: Complexity): Dispatcher

    /**
     * The remote [ClusterNode] currently joining the cluster.
     */
    val nodes: Collection<ClusterNode>
}
