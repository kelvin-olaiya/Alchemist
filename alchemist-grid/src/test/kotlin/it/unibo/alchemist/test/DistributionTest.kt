/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.assertions.nondeterministic.eventually
import io.kotest.assertions.nondeterministic.until
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.test.utils.DistributionTestUtils
import it.unibo.alchemist.test.utils.DistributionTestUtils.awaitServerJoin
import it.unibo.alchemist.test.utils.DistributionTestUtils.startClient
import it.unibo.alchemist.test.utils.DistributionTestUtils.startServers
import it.unibo.alchemist.test.utils.DistributionTestUtils.use
import it.unibo.alchemist.test.utils.TestConstants.SERVERS_TO_LAUNCH
import it.unibo.alchemist.test.utils.TestConstants.SIMULATION_BATCH_SIZE
import it.unibo.alchemist.test.utils.TestConstants.clientConfigFile
import it.unibo.alchemist.test.utils.TestConstants.clientConfigWithErrorFile
import it.unibo.alchemist.test.utils.TestConstants.composeFilePath
import it.unibo.alchemist.test.utils.TestConstants.registry
import it.unibo.alchemist.test.utils.TestConstants.serverConfigFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DistributionTest : StringSpec({

    extensions(DistributionTestUtils.getDockerExtension(composeFilePath))

    "Simulation are correctly distributed" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use {
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            startClient(clientConfigFile).use {
                until(20.seconds) {
                    registry.simulations().size == 1
                }
                val simulationID = registry.simulations().first()
                registry.simulationJobs(simulationID) shouldHaveSize SIMULATION_BATCH_SIZE
            }
        }
    }

    "Client should redistribute workload on node fault" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use { servers ->
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            startClient(clientConfigFile).use { _ ->
                until(20.seconds) {
                    registry.simulations().size == 1
                }
                val simulationID = registry.simulations().first()
                registry.simulationAssignments(simulationID).keys shouldHaveSize SERVERS_TO_LAUNCH
                servers.first().close()
                eventually(15.seconds) {
                    registry.simulationAssignments(simulationID).keys shouldHaveSize SERVERS_TO_LAUNCH - 1
                    registry.assignedJobs(registry.nodes.first().serverID) shouldHaveSize SIMULATION_BATCH_SIZE
                }
            }
        }
    }

    "Results must be made available locally" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use { _ ->
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            startClient(clientConfigFile).use {
                until(20.seconds) {
                    registry.simulations().size == 1
                }
                eventually(1.minutes) {
                    val results = Files.list(Path.of(".")).filter {
                        it.name.startsWith("time_export")
                    }.collect(Collectors.toList())
                    results shouldHaveSize SIMULATION_BATCH_SIZE
                    results.forEach { it.deleteIfExists() }
                }
            }
        }
    }

    "Batch with errors" {
        startServers(serverConfigFile, 2).use { _ ->
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            startClient(clientConfigWithErrorFile).use {
                until(20.seconds) {
                    registry.simulations().size == 1
                }
                eventually(1.minutes) {
                    it.stdoutAsText() shouldContain "Simulation batch encountered execution errors"
                }
            }
        }
    }
})
