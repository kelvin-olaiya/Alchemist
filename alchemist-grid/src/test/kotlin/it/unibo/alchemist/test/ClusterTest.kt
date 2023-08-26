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
import io.kotest.matchers.ints.shouldBeExactly
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.manager.ClusterManagerImpl
import it.unibo.alchemist.boundary.grid.cluster.manager.EtcdHelper
import it.unibo.alchemist.test.utils.GridTestUtils.getDockerExtension
import it.unibo.alchemist.test.utils.GridTestUtils.startAlchemistProcess
import it.unibo.alchemist.test.utils.TestableProcess
import java.nio.file.Path

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        val processes = startServers(SERVERS_TO_LAUNCH)
        processes.forEach { it.awaitOutputContains("Registered to cluster") }
        val cluster = ClusterImpl(ClusterManagerImpl(etcdHelper))
        cluster.servers.size shouldBeExactly SERVERS_TO_LAUNCH
        processes.forEach { it.close() }
    }

    "Cluster exposes correct number of servers after shutdown" {
        val processes = startServers(SERVERS_TO_LAUNCH)
        processes.forEach { it.awaitOutputContains("Registered to cluster") }
        processes.first().close()
        Thread.sleep(3000)
        val cluster = ClusterImpl(ClusterManagerImpl(etcdHelper))
        cluster.servers.size shouldBeExactly SERVERS_TO_LAUNCH - 1
        processes.forEach { it.close() }
    }
}) {
    companion object {
        private fun startServers(count: Int): List<TestableProcess> = (0 until count).map {
            startAlchemistProcess("run", serverConfigFile, "--verbosity", "debug")
        }.toList()
        private const val SERVERS_TO_LAUNCH = 2
        private val ETCD_SERVER_ENDPOINTS =
            listOf("http://localhost:10001", "http://localhost:10003", "http://localhost:10003")
        private val etcdHelper = EtcdHelper(ETCD_SERVER_ENDPOINTS)
        private val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
        private val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
    }
}
