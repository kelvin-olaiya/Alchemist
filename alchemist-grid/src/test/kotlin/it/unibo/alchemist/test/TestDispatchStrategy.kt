/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import it.unibo.alchemist.boundary.grid.cluster.ClusterNode
import it.unibo.alchemist.boundary.grid.cluster.DispatchStrategyFactory
import it.unibo.alchemist.boundary.grid.simulation.SimulationInitializer
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream

class TestDispatchStrategy : StringSpec({

    "Test round robin" {
        val servers = Stream.generate { UUID.randomUUID() }.limit(2).collect(Collectors.toList())
        val jobs = Stream.generate { UUID.randomUUID() }.limit(3).collect(Collectors.toList())
        val assignments = DispatchStrategyFactory.roundRobin.makeAssignments(
            servers.map { serverFromUUID(it) },
            jobs,
        )
        assignments.mapKeys { it.key.serverID } shouldBe mapOf(
            servers[0] to listOf(jobs[0], jobs[2]),
            servers[1] to listOf(jobs[1]),
        )
    }
}) {
    companion object {
        const val NUM_OF_SERVERS = 2L

        fun serverFromUUID(uuid: UUID) = object : ClusterNode {
            override val serverID: UUID
                get() = uuid
            override val metadata: Map<String, String>
                get() = throw NotImplementedError()

            override fun submitJob(simulationID: UUID, parameters: SimulationInitializer): UUID {
                throw NotImplementedError()
            }
        }
    }
}
