/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test.utils

import it.unibo.alchemist.boundary.grid.cluster.management.ObservableClusterRegistry
import it.unibo.alchemist.boundary.grid.cluster.storage.EtcdKVStore
import java.nio.file.Path

object TestConstants {
    const val SERVERS_TO_LAUNCH = 2
    val registry = ObservableClusterRegistry(
        EtcdKVStore(listOf("http://localhost:10001", "http://localhost:10003", "http://localhost:10003")),
    )
    val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
    val clientConfigFile = Path.of("src", "test", "resources", "client-config.yml").toString()
    val wrongExporterConfigFile = Path.of("src", "test", "resources", "wrong-exporter-config.yml").toString()
    const val SIMULATION_BATCH_SIZE = 4
    val distributionConfigurationFile =
        Path.of("src", "test", "resources", "distribution-config.yml").toString()
    val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
}
