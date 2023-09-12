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
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.test.utils.DistributionTestUtils.awaitServerJoin
import it.unibo.alchemist.test.utils.DistributionTestUtils.getDockerExtension
import it.unibo.alchemist.test.utils.DistributionTestUtils.startServers
import it.unibo.alchemist.test.utils.DistributionTestUtils.use
import it.unibo.alchemist.test.utils.TestConstants.SERVERS_TO_LAUNCH
import it.unibo.alchemist.test.utils.TestConstants.composeFilePath
import it.unibo.alchemist.test.utils.TestConstants.registry
import it.unibo.alchemist.test.utils.TestConstants.serverConfigFile
import kotlin.time.Duration.Companion.seconds

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use {
            val cluster = ClusterImpl(registry)
            eventually(10.seconds) {
                cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH
            }
        }
    }

    "Cluster exposes correct number of servers after shutdown" {
        startServers(serverConfigFile, SERVERS_TO_LAUNCH).use { servers ->
            val serverToShutdown = servers.first()
            val cluster = ClusterImpl(registry)
            awaitServerJoin(cluster, SERVERS_TO_LAUNCH, 10.seconds)
            serverToShutdown.close()
            eventually(10.seconds) {
                cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH - 1
            }
        }
    }
})
