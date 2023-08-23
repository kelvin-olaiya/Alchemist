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
import it.unibo.alchemist.test.utils.GridTestUtils.getDockerExtension
import it.unibo.alchemist.test.utils.GridTestUtils.startAlchemistProcess
import java.nio.file.Path

class ClusterTest : StringSpec({

    extensions(getDockerExtension(composeFilePath))

    "Cluster exposes correct number of servers" {
        startAlchemistProcess("run", serverConfigFile).waitFor()
        // cluster.servers.count() shouldBeExactly SERVERS_TO_LAUNCH
    }

    afterTest {
        println("Test custom after")
    }
}) {
    companion object {
        const val SERVERS_TO_LAUNCH = 2
        val serverConfigFile = Path.of("src", "test", "resources", "server-config.yml").toString()
        val composeFilePath = Path.of("src", "test", "resources", "docker-compose.yml").toString()
    }
}
