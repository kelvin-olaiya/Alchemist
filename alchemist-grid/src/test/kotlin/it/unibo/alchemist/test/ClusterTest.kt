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
import it.unibo.alchemist.boundary.LoadAlchemist
import it.unibo.alchemist.boundary.grid.AlchemistServer
import it.unibo.alchemist.boundary.grid.cluster.ClusterImpl
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import it.unibo.alchemist.boundary.launchers.DistributedExecution
import it.unibo.alchemist.test.utils.GridTestUtils.getDockerExtension
import java.nio.file.Path
import java.util.UUID

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        val servers = startServers(SERVERS_TO_LAUNCH)
        servers.forEach { it.register() }
        val cluster = ClusterImpl(etcdKVStore)
        cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH
    }

    "Cluster exposes correct number of servers after shutdown" {
        val servers = startServers(SERVERS_TO_LAUNCH)
        servers.forEach { it.register() }
        val serverToShutdown = servers.first()
        serverToShutdown.deregister()
        val cluster = ClusterImpl(etcdKVStore)
        cluster.nodes.size shouldBeExactly SERVERS_TO_LAUNCH - 1
    }

    "Simulation are correctly distributed" {
        val servers = startServers(SERVERS_TO_LAUNCH)
        servers.forEach { it.register() }
        val loader = LoadAlchemist.from(clientConfigFile)
        val client = DistributedExecution(listOf("horizontalEnd", "verticalEnd"))
        client.launch(loader)
        Thread.sleep(2000)
        servers.sumOf { it.runningSimulations } shouldBeExactly 9
    }
}) {
    companion object {
        private fun startServers(count: Int): List<AlchemistServer> = (0 until count)
            .map { UUID.randomUUID() }
            .map { AlchemistServer(it, etcdKVStore) }
            .toList()
        private const val SERVERS_TO_LAUNCH = 2
        private val ETCD_SERVER_ENDPOINTS =
            listOf("http://localhost:10001", "http://localhost:10003", "http://localhost:10003")
        private val etcdKVStore = EtcdKVStore(ETCD_SERVER_ENDPOINTS)
        private val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
        private val clientConfigFile = Path.of("src", "test", "resources", "client-config.yml").toString()
        private val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
    }
}
