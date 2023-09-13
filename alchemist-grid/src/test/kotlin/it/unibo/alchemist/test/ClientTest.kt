/*
 * Copyright (C) 2010-2023, Danilo Pianini and contributors
 * listed, for each module, in the respective subproject's build.gradle.kts file.
 *
 * This file is part of Alchemist, and is distributed under the terms of the
 * GNU General Public License, with a linking exception,
 * as described in the file LICENSE in the Alchemist distribution's top directory.
 */

package it.unibo.alchemist.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import it.unibo.alchemist.boundary.LoadAlchemist
import it.unibo.alchemist.boundary.grid.exceptions.NoAvailableServerException
import it.unibo.alchemist.boundary.launchers.DistributedExecution
import it.unibo.alchemist.test.utils.DistributionTestUtils
import it.unibo.alchemist.test.utils.TestConstants.clientConfigFile
import it.unibo.alchemist.test.utils.TestConstants.composeFilePath
import it.unibo.alchemist.test.utils.TestConstants.distributionConfigurationFile
import it.unibo.alchemist.test.utils.TestConstants.wrongExporterConfigFile

class ClientTest : StringSpec({

    extensions(DistributionTestUtils.getDockerExtension(composeFilePath))

    "Client should throw exception if no server is available" {
        val loader = LoadAlchemist.from(clientConfigFile)
        val client = DistributedExecution(distributionConfigurationFile, listOf("horizontalEnd", "verticalEnd"))
        shouldThrow<NoAvailableServerException> {
            client.launch(loader)
        }
    }

    "Only distributed exporter are allowed" {
        val loader = LoadAlchemist.from(wrongExporterConfigFile)
        val client = DistributedExecution(distributionConfigurationFile, listOf("horizontalEnd", "verticalEnd"))
        shouldThrow<IllegalArgumentException> {
            client.launch(loader)
        }
    }
})
