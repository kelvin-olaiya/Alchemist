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
import it.unibo.alchemist.boundary.grid.cluster.manager.ClusterEtcdManagerImpl
import it.unibo.alchemist.test.utils.GridTestUtils.getDockerExtension
import it.unibo.alchemist.test.utils.GridTestUtils.startAlchemistProcess
import java.nio.file.Path

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        val processes = (0 until SERVERS_TO_LAUNCH).map {
            startAlchemistProcess("run", serverConfigFile, "--verbosity", "debug")
        }.toList()
        processes.forEach { it.awaitOutputContains("Registered to cluster") }
        val servers = ClusterEtcdManagerImpl(ETCD_SERVER_ENDPOINTS).servers
        servers.size shouldBeExactly SERVERS_TO_LAUNCH
        processes.forEach { it.close() }
    }
}) {
    companion object {
        const val SERVERS_TO_LAUNCH = 2
        val ETCD_SERVER_ENDPOINTS = listOf("http://localhost:10001", "http://localhost:10003", "http://localhost:10003")
        val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
        val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
    }
}
