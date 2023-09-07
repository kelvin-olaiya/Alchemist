/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.boundary.grid.cluster

import java.util.UUID

object DispatchStrategyFactory {

    val roundRobin = object : DispatchStrategy {
        override fun makeAssignments(servers: List<ClusterNode>, jobs: List<UUID>): Map<ClusterNode, List<UUID>> {
            return servers.indices.map {
                it until jobs.size step servers.size
            }.withIndex().associate {
                servers[it.index] to it.value.map { jobIndex -> jobs[jobIndex] }.toList()
            }
        }
    }
}
