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
import io.kotest.matchers.ints.shouldBeExactly
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.test.utils.DistributionTestUtils
import it.unibo.alchemist.test.utils.DistributionTestUtils.startServers
import it.unibo.alchemist.test.utils.DistributionTestUtils.use
import it.unibo.alchemist.test.utils.TestConstants.BATCH_SIZE
import it.unibo.alchemist.test.utils.TestConstants.SERVERS_TO_LAUNCH
import it.unibo.alchemist.test.utils.TestConstants.clientConfigFile
import it.unibo.alchemist.test.utils.TestConstants.composeFilePath
import it.unibo.alchemist.test.utils.TestConstants.registry
import it.unibo.alchemist.test.utils.TestConstants.serverConfigFile
import kotlin.time.Duration.Companion.seconds

class DistributionTest : StringSpec({

    extensions(DistributionTestUtils.getDockerExtension(composeFilePath))

    "Simulation are correctly distributed" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use {
            val cluster = ClusterImpl(registry)
            DistributionTestUtils.awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            DistributionTestUtils.startClient(clientConfigFile).use {
                until(30.seconds) {
                    registry.simulations().size == 1
                }
                val simulationID = registry.simulations().first()
                registry.simulationJobs(simulationID) shouldHaveSize BATCH_SIZE
            }
        }
    }

    "Client should redistribute workload on node fault" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use { servers ->
            val cluster = ClusterImpl(registry)
            DistributionTestUtils.awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            DistributionTestUtils.startClient(clientConfigFile).use { _ ->
                until(30.seconds) {
                    registry.simulations().size == 1
                }
                val simulationID = registry.simulations().first()
                registry.simulationAssignments(simulationID).keys.size shouldBeExactly SERVERS_TO_LAUNCH
                servers.first().close()
                eventually(15.seconds) {
                    registry.simulationAssignments(simulationID).keys.size shouldBeExactly SERVERS_TO_LAUNCH - 1
                    registry.assignedJobs(registry.nodes.first().serverID) shouldHaveSize BATCH_SIZE
                }
            }
        }
    }
})
