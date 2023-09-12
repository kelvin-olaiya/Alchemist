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
import it.unibo.alchemist.boundary.grid.cluster.management.ObservableClusterRegistry
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.test.utils.GridTestUtils.getDockerExtension
import it.unibo.alchemist.test.utils.GridTestUtils.startAlchemistProcess
import it.unibo.alchemist.test.utils.TestableProcess
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        startServers(SERVERS_TO_LAUNCH)
        val cluster = ClusterImpl(registry)
        eventually(10.seconds) {
            cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH
        }
    }

    "Cluster exposes correct number of servers after shutdown" {
        val servers = startServers(SERVERS_TO_LAUNCH)
        val serverToShutdown = servers.first()
        val cluster = ClusterImpl(registry)
        until(10.seconds) {
            cluster.nodes.size == SERVERS_TO_LAUNCH
        }
        serverToShutdown.close()
        eventually(2.seconds) {
            cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH - 1
        }
    }

    "Simulation are correctly distributed" {
        startServers(SERVERS_TO_LAUNCH)
        val cluster = ClusterImpl(registry)
        until(10.seconds) {
            cluster.nodes.size == SERVERS_TO_LAUNCH
        }
        startClient()
        val simulations = registry.simulations()
        until(30.seconds) {
            simulations.size == 1
        }
        val simulationID = simulations.first()
        registry.simulationJobs(simulationID) shouldHaveSize 9
    }
}) {
    companion object {
        private fun startServers(count: Int): List<TestableProcess> = (0 until count)
            .map {
                startAlchemistProcess("run", serverConfigFile, "--verbosity", "debug")
            }.toList()
        private fun startClient(): TestableProcess =
            startAlchemistProcess("run", clientConfigFile, "--verbosity", "debug")
        private const val SERVERS_TO_LAUNCH = 2
        private val ETCD_SERVER_ENDPOINTS =
            listOf("http://localhost:10001", "http://localhost:10003", "http://localhost:10003")
        private val etcdKVStore = EtcdKVStore(ETCD_SERVER_ENDPOINTS)
        private val registry = ObservableClusterRegistry(etcdKVStore)
        private val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
        private val clientConfigFile = Path.of("src", "test", "resources", "client-config.yml").toString()
        private val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
    }
}
